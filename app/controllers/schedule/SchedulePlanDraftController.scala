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
import models.schedule.ScheduleEntryProtocol
import models.schedule.ScheduleEntrySeriesId
import models.Semester
import permission.SchedulePlanningCheck
import permission.SchedulePlanningViewCheck
import play.api.libs.json.Json
import play.api.mvc.*
import security.ClientErrorResponse

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
    with SchedulePlanningCheck
    with SchedulePlanningViewCheck {

  /**
   * Returns all plan drafts, filtered by semester, kind or active status.
   *
   * Query parameters:
   *   - `semester`: the semester to filter by (for example `"wise_2025"`)
   *   - `kind`: the kind of plan draft to filter by (for example `"schedule"`)
   *   - `activeOnly`: whether to filter by active status only (for example `"true"`)
   */
  def all() =
    auth.andThen(resolveUser).andThen(canViewPlanDraft).async { (r: UserRequest[AnyContent]) =>
      val result = for {
        semester   <- parseSemester(r.getQueryString("semester"))
        kind       <- parseKind(r.getQueryString("kind"))
        activeOnly <- parseActive(r.getQueryString("activeOnly"))
      } yield repo.all(kind, semester, activeOnly).map(drafts => Ok(Json.toJson(drafts)))

      result.fold(Future.successful, _.recover(clientError))
    }

  def get(id: UUID) =
    auth.andThen(resolveUser).andThen(canViewPlanDraft).async { r =>
      val result = for {
        kind <- parseKind(r.getQueryString("kind"))
      } yield repo.get(id, kind).map {
        case Some((d, s)) => Ok(Json.obj("planDraft" -> d, "semester" -> s))
        case None         => NotFound
      }
      result.fold(Future.successful, _.recover(clientError))
    }

  def create() =
    auth(parse.json[PlanDraftProtocol]).andThen(resolveUser).andThen(canUpdatePlanDraft).async {
      (r: UserRequest[PlanDraftProtocol]) => repo.create(r.body).map(_ => Created).recover(clientError)
    }

  /**
   * Only active (not published) plan drafts can be deleted.
   * Associated schedule entry drafts are also deleted.
   */
  def delete(id: UUID) =
    auth.andThen(resolveUser).andThen(canUpdatePlanDraft).async { (_: UserRequest[AnyContent]) =>
      repo.deleteActive(id).map(if _ then NoContent else NotFound).recover(clientError)
    }

  /**
   * Returns schedule entry drafts for the given plan draft and date range.
   * Throws an exception if the plan draft id is not a schedule draft.
   *
   * Accepts the same `semester` or `from` and `to` query parameters as `ScheduleEntryController.all`.
   */
  def scheduleEntriesDrafts(planDraftId: UUID) =
    auth.andThen(resolveUser).andThen(canViewPlanDraft).async { (r: UserRequest[AnyContent]) =>
      ScheduleDateRange.resolve(r) match {
        case Left(result)      => Future.successful(result)
        case Right((from, to)) => repo.scheduleEntriesDrafts(planDraftId, from, to).map(Ok(_)).recover(clientError)
      }
    }

  /**
   * Creates new schedule entry drafts for the given plan draft and returns the created entries as JSON.
   * Throws an exception if the plan draft id is not a schedule draft.
   */
  def createScheduleEntriesDrafts(planDraftId: UUID) =
    auth(parse.json[List[ScheduleEntryProtocol]])
      .andThen(resolveUser)
      .andThen(canUpdatePlanDraft)
      .async { (r: UserRequest[List[ScheduleEntryProtocol]]) =>
        repo.createScheduleEntriesDrafts(planDraftId, r.body).map(Created(_)).recover(clientError)
      }

  /**
   * Updates an existing schedule entry draft for the given plan draft and entry id.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def updateScheduleEntryDraft(planDraftId: UUID, entryId: UUID) =
    auth(parse.json[ScheduleEntryProtocol]).andThen(resolveUser).andThen(canUpdatePlanDraft).async {
      (r: UserRequest[ScheduleEntryProtocol]) =>
        repo.updateScheduleEntryDraft(planDraftId, entryId, r.body).map(Ok(_)).recover(clientError)
    }

  /** Updates every schedule entry draft in the same series as `entryId`. */
  def updateScheduleEntryDraftSeries(planDraftId: UUID, entryId: UUID) =
    auth(parse.json[ScheduleEntryProtocol]).andThen(resolveUser).andThen(canUpdatePlanDraft).async {
      (r: UserRequest[ScheduleEntryProtocol]) =>
        repo.updateScheduleEntryDraftSeries(planDraftId, entryId, r.body).map(Ok(_)).recover(clientError)
    }

  /**
   * Deletes an existing schedule entry draft for the given plan draft and entry id.
   * Throws an exception if the plan draft id is not an active schedule draft.
   */
  def deleteScheduleEntryDraft(planDraftId: UUID, entryId: UUID) =
    auth.andThen(resolveUser).andThen(canUpdatePlanDraft).async { (_: UserRequest[AnyContent]) =>
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
    auth.andThen(resolveUser).andThen(canUpdatePlanDraft).async { (_: UserRequest[AnyContent]) =>
      repo.publish(id).map(_ => Created).recover(clientError)
    }

  /** Checks whether a schedule entry draft series exists for `seriesId` and returns the series data. */
  def getSeriesOccurrences(planDraftId: UUID, seriesId: UUID) =
    auth.andThen(resolveUser).andThen(canUpdatePlanDraft).async { (_: UserRequest[AnyContent]) =>
      repo
        .hasSeries(planDraftId, ScheduleEntrySeriesId(seriesId))
        .map(res => Ok(Json.toJson(res)))
        .recover(clientError)
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
    case _: NoSuchElementException   => NotFound
    case e: IllegalArgumentException => BadRequest(Json.obj("message" -> e.getMessage))
  }
}
