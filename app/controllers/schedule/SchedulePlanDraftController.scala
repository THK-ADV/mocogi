package controllers.schedule

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.schedule.SchedulePlanDraftRepository
import database.repo.PermissionRepository
import models.schedule.PlanDraftKind
import models.schedule.PlanDraftProtocol
import models.schedule.ScheduleEntryDraft
import models.Semester
import permission.SchedulePlanningCheck
import play.api.libs.json.Json
import play.api.mvc.*
import security.ClientErrorResponse
import org.postgresql.util.PSQLException

@Singleton
final class SchedulePlanDraftController @Inject() (
    cc: ControllerComponents,
    repo: SchedulePlanDraftRepository,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction
    with SchedulePlanningCheck {

  /**
   * Returns all plan drafts, filtered by semester, kind or active status.
   *
   * Query parameters:
   *   - `semester`: the semester to filter by (for example `"wise_2025"`)
   *   - `kind`: the kind of plan draft to filter by (for example `"schedule"`)
   *   - `activeOnly`: whether to filter by active status only (for example `"true"`)
   */
  def all() =
    auth.andThen(resolveUser).andThen(hasSchedulePlanningPermission).async { (r: UserRequest[AnyContent]) =>
      val result = for {
        semester   <- parseSemester(r.getQueryString("semester"))
        kind       <- parseKind(r.getQueryString("kind"))
        activeOnly <- parseActive(r.getQueryString("activeOnly"))
      } yield repo.all(kind, semester, activeOnly).map(drafts => Ok(Json.toJson(drafts)))

      result.fold(Future.successful, _.recover(clientError))
    }

  def get(id: UUID) =
    auth.andThen(resolveUser).andThen(hasSchedulePlanningPermission).async { r =>
      val result = for {
        kind <- parseKind(r.getQueryString("kind"))
      } yield repo.get(id, kind).map {
        case Some((d, s)) => Ok(Json.obj("planDraft" -> d, "semester" -> s))
        case None         => NotFound
      }
      result.fold(Future.successful, _.recover(clientError))
    }

  def create() =
    auth(parse.json[PlanDraftProtocol]).andThen(resolveUser).andThen(hasSchedulePlanningPermission).async {
      (r: UserRequest[PlanDraftProtocol]) => repo.create(r.body).map(_ => Created).recover(clientError)
    }

  /**
   * Only active (not published) plan drafts can be deleted.
   * Associated schedule entry drafts are also deleted.
   */
  def delete(id: UUID) =
    auth.andThen(resolveUser).andThen(hasSchedulePlanningPermission).async { (_: UserRequest[AnyContent]) =>
      repo.deleteActive(id).map(_ => NoContent).recover(clientError)
    }

  /**
   * Returns all schedule entry drafts for the given plan draft.
   * Throws an exception if the plan draft id is not a schedule draft.
   */
  def scheduleEntriesDrafts(planDraftId: UUID) =
    auth.andThen(resolveUser).andThen(hasSchedulePlanningPermission).async { (_: UserRequest[AnyContent]) =>
      repo.scheduleEntriesDrafts(planDraftId).map(Ok(_)).recover(clientError)
    }

  /**
   * Creates new schedule entry drafts for the given plan draft.
   * Throws an exception if the plan draft id is not a schedule draft.
   */
  def createScheduleEntriesDrafts(planDraftId: UUID) =
    auth(parse.json[List[ScheduleEntryDraft.JSON]]).andThen(resolveUser).andThen(hasSchedulePlanningPermission).async {
      (r: UserRequest[List[ScheduleEntryDraft.JSON]]) =>
        repo.createScheduleEntriesDrafts(planDraftId, r.body).map(_ => Created).recover(clientError)
    }

  /**
   * Updates an existing schedule entry draft for the given plan draft and entry id.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def updateScheduleEntryDraft(planDraftId: UUID, entryId: UUID) =
    auth(parse.json[ScheduleEntryDraft.JSON]).andThen(resolveUser).andThen(hasSchedulePlanningPermission).async {
      (r: UserRequest[ScheduleEntryDraft.JSON]) =>
        if planDraftId != r.body.planDraft then
          Future.successful(
            BadRequest(Json.obj("message" -> "plan draft id does not match the plan draft id in the payload"))
          )
        else repo.updateScheduleEntryDraft(entryId, r.body).map(_ => NoContent).recover(clientError)
    }

  /**
   * Deletes an existing schedule entry draft for the given plan draft and entry id.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def deleteScheduleEntryDraft(planDraftId: UUID, entryId: UUID) =
    auth.andThen(resolveUser).andThen(hasSchedulePlanningPermission).async { (_: UserRequest[AnyContent]) =>
      repo
        .deleteScheduleEntryDraft(planDraftId, entryId)
        .map(if _ then NoContent else NotFound)
        .recover(clientError)
    }

  /**
   * Publishes an active schedule plan draft by copying every entry draft into a live schedule entry.
   * Throws if the plan draft is missing, not a schedule draft, already published, or has no entry drafts.
   */
  def publish(id: UUID) =
    auth.andThen(resolveUser).andThen(hasSchedulePlanningPermission).async { (_: UserRequest[AnyContent]) =>
      repo.publish(id).map(_ => Created).recover(clientError)
    }

  private def parseSemester(raw: Option[String]): Either[Result, Option[String]] =
    raw match {
      case Some(value) =>
        try Right(Some(Semester.apply(value).id))
        catch case NonFatal(_) => Left(BadRequest(Json.obj("message" -> s"invalid semester: $value")))
      case None => Right(None)
    }

  private def parseKind(raw: Option[String]): Either[Result, PlanDraftKind] =
    raw match {
      case Some(value) =>
        try Right(PlanDraftKind(value))
        catch case NonFatal(_) => Left(BadRequest(Json.obj("message" -> s"invalid kind: $value")))
      case None => Left(BadRequest(Json.obj("message" -> "kind query parameter is required")))
    }

  private def parseActive(raw: Option[String]): Either[Result, Boolean] =
    raw match {
      case Some(value) =>
        value.toBooleanOption.toRight(BadRequest(Json.obj("message" -> s"invalid activeOnly: $value")))
      case None => Right(false)
    }

  private def clientError: PartialFunction[Throwable, Result] = {
    case e: IllegalArgumentException => BadRequest(Json.obj("message" -> e.getMessage))
    case e: PSQLException            => BadRequest(Json.obj("message" -> e.getMessage))
  }
}
