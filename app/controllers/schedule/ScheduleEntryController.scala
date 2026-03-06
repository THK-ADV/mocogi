package controllers.schedule

import java.sql.Timestamp
import java.time.Instant
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
import models.schedule.ScheduleEntry
import models.Semester
import permission.SchedulePlanningCheck
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.mvc.*

@Singleton
final class ScheduleEntryController @Inject() (
    cc: ControllerComponents,
    repo: ScheduleEntryRepository,
    cached: Cached,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserResolveAction
    with SchedulePlanningCheck {

  private def parseDate(key: String, r: Request[AnyContent]): Option[Timestamp] =
    r.getQueryString(key).map(a => Timestamp.from(Instant.ofEpochMilli(a.toLong)))

  private def resolveDateRange(r: Request[AnyContent]): Either[Result, (Timestamp, Timestamp)] =
    r.getQueryString("semester") match {
      case Some(semester) =>
        val (from, to) = Semester.dateRange(semester)
        Right((Timestamp.valueOf(from), Timestamp.valueOf(to)))
      case None =>
        (parseDate("from", r), parseDate("to", r)) match {
          case (Some(from), Some(to)) => Right((from, to))
          case (None, None)           =>
            Left(BadRequest("Either `semester` or both `from` and `to` query parameters must be provided."))
          case _ =>
            Left(BadRequest("Both `from` and `to` query parameters must be provided together."))
        }
    }

  private val allAction =
    Action.async { (r: Request[AnyContent]) =>
      resolveDateRange(r) match {
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
    auth(parse.json[List[ScheduleEntry.JSON]]).andThen(resolveUser).andThen(hasSchedulePlanningPermission).async {
      (r: UserRequest[List[ScheduleEntry.JSON]]) =>
        repo.create(r.body.map(_.copy(id = UUID.randomUUID()))).map(Created(_))
    }

  /** Updates an existing schedule entry identified by `id` with the provided JSON payload. */
  def update(id: UUID) =
    auth(parse.json[ScheduleEntry.JSON]).andThen(resolveUser).andThen(hasSchedulePlanningPermission).async {
      (r: UserRequest[ScheduleEntry.JSON]) =>
        repo.update(r.body.copy(id = id)).map(Ok(_))
    }

  /** Deletes the schedule entry identified by `id`. */
  def delete(id: UUID) =
    auth.andThen(resolveUser).andThen(hasSchedulePlanningPermission).async { (r: UserRequest[AnyContent]) =>
      repo.delete(id).map(_ => NoContent)
    }
}
