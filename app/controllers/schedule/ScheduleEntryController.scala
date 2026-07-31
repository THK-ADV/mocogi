package controllers.schedule

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.schedule.ScheduleEntryRepository
import database.repo.PermissionRepository
import models.schedule.ScheduleEntryProtocol
import models.schedule.ScheduleEntrySeriesId
import models.Semester
import permission.SchedulePlanningCheck
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.mvc.*
import security.ClientErrorResponse

@Singleton
final class ScheduleEntryController @Inject() (
    cc: ControllerComponents,
    repo: ScheduleEntryRepository,
    cached: Cached,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction
    with SchedulePlanningCheck {

  private val allAction =
    Action.async { (r: Request[AnyContent]) =>
      ScheduleDateRange.resolve(r) match {
        case Left(result) =>
          Future.successful(result)
        case Right((from, to)) =>
          repo.scheduleEntriesByRange(from, to).map(Ok(_))
      }
    }

  /** Returns the current and next semesters used for schedule planning. */
  def semesters() =
    Action { (r: Request[AnyContent]) =>
      Ok(Json.toJson(Semester.currentAndNext()))
    }

  /**
   * Returns all schedule entries, filtered either by semester or by an explicit date range.
   *
   * Query parameters:
   *   - `semester`: when present, filters schedule entries for the given semester identifier
   *                 (for example `"wise_2025"`). This takes precedence over `from` / `to`.
   *   - `from` and `to`: when both are present (as epoch millisecond timestamps), filters
   *                      schedule entries within that date range.
   */
  def all(): EssentialAction =
    EssentialAction { r =>
      if r.headers.get("Cache-Control").contains("no-cache") then allAction(r)
      else cached.status(r => r.method + r.uri, 200, 15.minutes)(allAction)(r)
    }

  /** Creates new schedule entries from the JSON payload and returns the created entries as JSON. */
  def create() =
    auth(parse.json[List[ScheduleEntryProtocol]]).andThen(resolveUser).andThen(canUpdatePlanDraft).async {
      (r: UserRequest[List[ScheduleEntryProtocol]]) =>
        repo.create(r.body).map(Created(_))
    }

  /** Updates an existing schedule entry identified by `id` with the provided JSON payload. */
  def update(id: UUID) =
    auth(parse.json[ScheduleEntryProtocol]).andThen(resolveUser).andThen(canUpdatePlanDraft).async {
      (r: UserRequest[ScheduleEntryProtocol]) =>
        repo.update(id, r.body).map(Ok(_)).recover(clientError)
    }

  /** Updates every schedule entry in the same series as `id` with the provided JSON payload. */
  def updateSeries(id: UUID) =
    auth(parse.json[ScheduleEntryProtocol]).andThen(resolveUser).andThen(canUpdatePlanDraft).async {
      (r: UserRequest[ScheduleEntryProtocol]) =>
        repo.updateSeries(id, r.body).map(Ok(_)).recover(clientError)
    }

  /** Checks whether a schedule entry series exists for `seriesID` and returns the series data */
  def getSeriesOccurrences(seriesID: UUID) =
    auth.andThen(resolveUser).andThen(canUpdatePlanDraft).async { _ =>
      repo
        .hasSeries(ScheduleEntrySeriesId(seriesID))
        .map(res => Ok(Json.toJson(res)))
    }

  /** Deletes the schedule entry identified by `id`. */
  def delete(id: UUID) =
    auth.andThen(resolveUser).andThen(canUpdatePlanDraft).async { (r: UserRequest[AnyContent]) =>
      repo.delete(id).map(if _ then NoContent else NotFound)
    }

  private def clientError: PartialFunction[Throwable, Result] = {
    case _: NoSuchElementException   => NotFound
    case e: IllegalArgumentException => BadRequest(Json.obj("message" -> e.getMessage))
  }
}
