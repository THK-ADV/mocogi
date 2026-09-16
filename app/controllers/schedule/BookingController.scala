package controllers.schedule

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Success
import scala.util.Try

import auth.AuthorizationAction
import auth.Token
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import controllers.ResourceCache
import database.repo.schedule.BookingRepository
import database.repo.PermissionRepository
import models.schedule.BookingKind
import models.schedule.BookingProtocol
import models.schedule.ScheduleEntrySeriesId
import permission.ScheduleBookingCheck
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.Logging
import security.ClientErrorResponse

@Singleton
final class BookingController @Inject() (
    cc: ControllerComponents,
    repo: BookingRepository,
    cache: ResourceCache,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction
    with ScheduleBookingCheck
    with Logging {

  private def canSeeFacultyBookings(token: Token): Boolean =
    token.roles.contains("employee")

  private def read(kind: BookingKind, request: RequestHeader): Future[Result] =
    Try(ScheduleDateRange.resolve(request)).toEither match {
      case Left(_)                  => Future.successful(BadRequest("Invalid semester or date range"))
      case Right(Left(result))      => Future.successful(result)
      case Right(Right((from, to))) => repo.bookingsByRange(kind, from, to).map(Ok(_).as(JSON))
    }

  def all(): EssentialAction = EssentialAction { request =>
    val kind = request.queryString
      .get("kind")
      .collect { case Seq(value) => value }
      .flatMap(value => BookingKind.values.find(_.id == value))
    kind match {
      case Some(BookingKind.Faculty) =>
        // This response depends on Authorization: never pass it through ResourceCache/Cached.
        auth.async { r =>
          val result =
            if canSeeFacultyBookings(r.token) then read(BookingKind.Faculty, r)
            else Future.successful(Forbidden)
          result.map(_.withHeaders(CACHE_CONTROL -> "no-store"))
        }(request)
      case Some(kind) =>
        cache("bookings", 15.minutes)(Action.async(r => read(kind, r)))(request)
      case _ =>
        Action(BadRequest("Exactly one kind query parameter is required: teaching, campus or faculty"))(request)
    }
  }

  /** Accepts a JSON array; occurrences in one series share seriesId and kind. */
  def create() =
    auth(parse.json[List[BookingProtocol]]).andThen(resolveUser).andThen(canUpdateBookings).async {
      (r: UserRequest[List[BookingProtocol]]) =>
        val actor = actorId(r)
        repo
          .create(r.body, actor)
          .andThen(invalidate)
          .andThen(logCreated(actor))
          .map(Created(_).as(JSON))
          .recover(clientError)
    }

  def update(id: UUID) =
    auth(parse.json[BookingProtocol]).andThen(resolveUser).andThen(canUpdateBookings).async {
      (r: UserRequest[BookingProtocol]) =>
        repo
          .update(id, r.body)
          .andThen(invalidate)
          .andThen(logUpdated(actorId(r), id))
          .map(Ok(_).as(JSON))
          .recover(clientError)
    }

  def updateSeries(id: UUID) =
    auth(parse.json[BookingProtocol]).andThen(resolveUser).andThen(canUpdateBookings).async {
      (r: UserRequest[BookingProtocol]) =>
        repo
          .updateSeries(id, r.body)
          .andThen(invalidate)
          .andThen(logSeriesUpdated(actorId(r), id))
          .map(Ok(_).as(JSON))
          .recover(clientError)
    }

  def getSeriesOccurrences(seriesId: UUID) =
    auth.andThen(resolveUser).andThen(canUpdateBookings).async { _ =>
      repo.hasSeries(ScheduleEntrySeriesId(seriesId)).map(entries => Ok(Json.toJson(entries)))
    }

  def delete(id: UUID) =
    auth.andThen(resolveUser).andThen(canUpdateBookings).async { r =>
      repo
        .delete(id)
        .andThen(invalidate)
        .andThen(logDeleted(actorId(r), id))
        .map(if _ then NoContent else NotFound)
    }

  private def actorId[A](request: UserRequest[A]): String =
    if request.person.id.nonEmpty then request.person.id else request.request.token.username

  private def bookingIds(json: String): Seq[UUID] =
    Try(Json.parse(json)).toOption
      .flatMap(_.asOpt[JsArray])
      .toSeq
      .flatMap(_.value.flatMap(js => (js \ "id").asOpt[UUID]))

  private def logCreated(actor: String): PartialFunction[Try[String], Unit] = {
    case Success(json) => logger.info(s"booking created actor=$actor ids=${bookingIds(json).mkString(",")}")
  }

  private def logUpdated(actor: String, id: UUID): PartialFunction[Try[String], Unit] = {
    case Success(_) => logger.info(s"booking updated actor=$actor id=$id")
  }

  private def logSeriesUpdated(actor: String, anchorId: UUID): PartialFunction[Try[String], Unit] = {
    case Success(json) =>
      logger.info(s"booking series updated actor=$actor anchor=$anchorId ids=${bookingIds(json).mkString(",")}")
  }

  private def logDeleted(actor: String, id: UUID): PartialFunction[Try[Boolean], Unit] = {
    case Success(true) => logger.info(s"booking deleted actor=$actor id=$id")
  }

  private def invalidate[A]: PartialFunction[Try[A], Unit] = {
    case Success(_) => cache.invalidate("bookings")
  }

  private def clientError: PartialFunction[Throwable, Result] = {
    case _: NoSuchElementException   => NotFound
    case e: IllegalArgumentException => BadRequest(Json.obj("message" -> e.getMessage))
  }
}
