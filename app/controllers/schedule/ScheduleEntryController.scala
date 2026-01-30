package controllers.schedule

import java.sql.Timestamp
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import controllers.actions.UserRequest
import controllers.actions.UserResolveAction
import database.repo.schedule.ScheduleEntryRepository
import database.repo.PermissionRepository
import permission.AdminCheck
import play.api.cache.Cached
import play.api.libs.json.JsArray
import play.api.libs.json.JsValue
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request

@Singleton
final class ScheduleEntryController @Inject() (
    cc: ControllerComponents,
    repo: ScheduleEntryRepository,
    cached: Cached,
    auth: AuthorizationAction,
    val permissionRepository: PermissionRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with UserResolveAction {

  private def parseDate(key: String, r: Request[AnyContent]): Option[Timestamp] =
    r.getQueryString(key).map(a => Timestamp.from(Instant.ofEpochMilli(a.toLong)))

  private def parseDateRange(r: Request[AnyContent]): (Timestamp, Timestamp) =
    (parseDate("from", r), parseDate("to", r)) match {
      case (Some(from), Some(to)) => (from, to)
      case _                      => throw Exception("expect from and to parameter to be timestamps")
    }

  def all() =
    cached.status(r => r.method + r.uri, 200, 15.minutes) {
      Action.async { (r: Request[AnyContent]) =>
        println(r.method + r.uri)
        val (from, to) = parseDateRange(r)
        repo.scheduleEntriesByRange(from, to).map(Ok(_))
      }
    }

  // Create schedule entries from JSON
  def create() =
    auth(parse.json).andThen(resolveUser).andThen(isAdmin).async { (r: UserRequest[JsValue]) =>
      val json = r.body.validate[JsArray].get.value
      repo.createFromJson(json.toVector).map(_ => NoContent)
    }

  // Recreate module teaching unit association based on the module's PO relation
  def recreateModuleTeachingUnitAssociation() =
    auth.andThen(resolveUser).andThen(isAdmin).async { _ =>
      repo.bootstrapModuleTeachingUnit().map(_ => NoContent)
    }
}
