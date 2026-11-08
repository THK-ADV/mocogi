package controllers.schedule

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import cats.syntax.either.*
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.schedule.ExamPlanRepository
import database.repo.PermissionRepository
import models.schedule.ExamEntryCreate
import models.schedule.ExamEntryUpdate
import models.schedule.ExamPoolConfig
import models.schedule.ExamRoomAllocation
import models.schedule.InvigilationDutyRef
import models.schedule.PlanDraftProtocol
import models.Semester
import permission.ExamPlanningCheck
import permission.ExamPlanningViewCheck
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.mvc.*
import security.ClientErrorResponse

/** Exam plan drafts with their planning pool and appointments, and the published exam appointments. */
@Singleton
final class ExamPlanController @Inject() (
    cc: ControllerComponents,
    repo: ExamPlanRepository,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction
    with ExamPlanningCheck
    with ExamPlanningViewCheck {

  /** Returns the exam plan drafts, optionally only the one of the `semester`. */
  def all() =
    auth.andThen(resolveUser).andThen(canViewExamPlan).async { (r: UserRequest[AnyContent]) =>
      semester(r.getQueryString("semester")).fold(Future.successful, repo.all(_).map(p => Ok(Json.toJson(p))))
    }

  /** Returns the exam plan draft with its semester and whether it differs from its published copies. */
  def get(id: UUID) =
    auth.andThen(resolveUser).andThen(canViewExamPlan).async { (_: UserRequest[AnyContent]) =>
      repo.get(id).map {
        case Some((plan, semester, changes)) =>
          Ok(Json.obj("planDraft" -> plan, "semester" -> semester, "hasUnpublishedChanges" -> changes))
        case None => NotFound
      }
    }

  def create() =
    auth(parse.json[PlanDraftProtocol]).andThen(resolveUser).andThen(canUpdateExamPlan).async {
      (r: UserRequest[PlanDraftProtocol]) => repo.create(r.body).map(_ => Created).recover(clientError)
    }

  /** Deletes the exam plan draft with its appointments. A published exam plan responds with a conflict. */
  def delete(id: UUID) =
    auth.andThen(resolveUser).andThen(canUpdateExamPlan).async { (_: UserRequest[AnyContent]) =>
      repo
        .delete(id)
        .map(if _ then NoContent else Conflict(Json.obj("message" -> "a published exam plan cannot be deleted")))
        .recover(clientError)
    }

  /** Publishes the current state of the exam plan draft, which stays editable and can be published again. */
  def publish(id: UUID) =
    auth.andThen(resolveUser).andThen(canUpdateExamPlan).async { (_: UserRequest[AnyContent]) =>
      repo.publish(id).map(_ => NoContent).recover(clientError)
    }

  /** Returns the planning pool selected by the `config` query parameter, a URL encoded JSON `ExamPoolConfig`. */
  def pool() =
    auth.andThen(resolveUser).andThen(canViewExamPlan).async { (r: UserRequest[AnyContent]) =>
      r.getQueryString("config")
        .flatMap(c => Either.catchNonFatal(Json.parse(c)).toOption)
        .map(_.validate[ExamPoolConfig]) match {
        case Some(JsSuccess(c, _)) => repo.pool(c).map(Ok(_)).recover(clientError)
        case Some(e: JsError)      => Future.successful(badRequest(s"invalid config: ${JsError.toJson(e)}"))
        case None                  => Future.successful(badRequest("config query parameter must be a JSON pool config"))
      }
    }

  /** Returns the shared pool configuration of the exam plan draft. */
  def poolConfig(planDraft: UUID) =
    auth.andThen(resolveUser).andThen(canViewExamPlan).async { (_: UserRequest[AnyContent]) =>
      repo.poolConfig(planDraft).map(c => Ok(Json.toJson(c))).recover(clientError)
    }

  /** Replaces the shared pool configuration of the exam plan draft and returns it. */
  def updatePoolConfig(planDraft: UUID) =
    auth(parse.json[ExamPoolConfig]).andThen(resolveUser).andThen(canUpdateExamPlan).async {
      (r: UserRequest[ExamPoolConfig]) =>
        repo.setPoolConfig(planDraft, r.body).map(c => Ok(Json.toJson(c))).recover(clientError)
    }

  /** Returns the published appointments of the `semester` and/or overlapping `from` and `to` (epoch milliseconds). */
  def published() =
    Action.async { (r: Request[AnyContent]) =>
      (for {
        s <- semester(r.getQueryString("semester"))
        i <- interval(r)
        _ <- Either.raiseUnless(s.nonEmpty || i.nonEmpty)(badRequest("either semester or from and to are required"))
      } yield repo.published(s, i).map(Ok(_))).fold(Future.successful, identity)
    }

  /** Returns the appointments of the exam plan draft, optionally only those overlapping `from` and `to`. */
  def entries(planDraft: UUID) =
    auth.andThen(resolveUser).andThen(canViewExamPlan).async { (r: UserRequest[AnyContent]) =>
      interval(r).fold(Future.successful, repo.drafts(planDraft, _).map(Ok(_)).recover(clientError))
    }

  /** Creates appointments of existing exams and returns them. */
  def createEntries(planDraft: UUID) =
    auth(parse.json[List[ExamEntryCreate]]).andThen(resolveUser).andThen(canUpdateExamPlan).async {
      (r: UserRequest[List[ExamEntryCreate]]) =>
        repo.createEntries(planDraft, r.body).map(Created(_)).recover(clientError)
    }

  /** Replaces the planning fields of the appointment, keeping its rooms, and returns it. */
  def updateEntry(planDraft: UUID, entry: UUID) =
    auth(parse.json[ExamEntryUpdate]).andThen(resolveUser).andThen(canUpdateExamPlan).async {
      (r: UserRequest[ExamEntryUpdate]) => repo.updateEntry(planDraft, entry, r.body).map(Ok(_)).recover(clientError)
    }

  /** Replaces the room allocations of the appointment and returns it. */
  def updateRooms(planDraft: UUID, entry: UUID) =
    auth(parse.json[List[ExamRoomAllocation]]).andThen(resolveUser).andThen(canUpdateExamPlan).async {
      (r: UserRequest[List[ExamRoomAllocation]]) =>
        repo.setRooms(planDraft, entry, r.body).map(Ok(_)).recover(clientError)
    }

  /** Fills the open invigilator demand of the selected rooms and returns all appointments of the exam plan draft. */
  def assignInvigilators(planDraft: UUID) =
    auth(parse.json[List[InvigilationDutyRef]]).andThen(resolveUser).andThen(canUpdateExamPlan).async {
      (r: UserRequest[List[InvigilationDutyRef]]) =>
        repo.assignInvigilators(planDraft, r.body.toSet).map(Ok(_)).recover(clientError)
    }

  def deleteEntry(planDraft: UUID, entry: UUID) =
    auth.andThen(resolveUser).andThen(canUpdateExamPlan).async { (_: UserRequest[AnyContent]) =>
      repo.deleteEntry(planDraft, entry).map(if _ then NoContent else NotFound).recover(clientError)
    }

  private def semester(raw: Option[String]): Either[Result, Option[String]] =
    raw match {
      case Some(value) =>
        try Right(Some(Semester(value).id))
        catch case NonFatal(_) => Left(badRequest(s"invalid semester: $value"))
      case None => Right(None)
    }

  private def interval(r: RequestHeader): Either[Result, Option[(Instant, Instant)]] =
    (r.getQueryString("from").map(_.toLongOption), r.getQueryString("to").map(_.toLongOption)) match {
      case (None, None)                                    => Right(None)
      case (Some(Some(from)), Some(Some(to))) if to > from =>
        Right(Some((Instant.ofEpochMilli(from), Instant.ofEpochMilli(to))))
      case _ => Left(badRequest("from and to must both be epoch milliseconds with to after from"))
    }

  private def badRequest(message: String) = BadRequest(Json.obj("message" -> message))

  private def clientError: PartialFunction[Throwable, Result] = {
    case e: NoSuchElementException   => NotFound(Json.obj("message" -> e.getMessage))
    case e: IllegalArgumentException => badRequest(e.getMessage)
  }
}
