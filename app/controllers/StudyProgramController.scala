package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import database.view.StudyProgramViewRepository
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request

private enum StudyProgramFilter {
  case NotExpired      extends StudyProgramFilter
  case CurrentlyActive extends StudyProgramFilter
}

@Singleton
final class StudyProgramController @Inject() (
    cc: ControllerComponents,
    studyProgramViewRepo: StudyProgramViewRepository,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  import StudyProgramFilter.*

  private def parseFilter(request: Request[AnyContent]): StudyProgramFilter =
    request.getQueryString("filter") match {
      case Some("not-expired")      => NotExpired
      case Some("currently-active") => CurrentlyActive
      case _                        => NotExpired
    }

  def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { request =>
        val res = parseFilter(request) match {
          case NotExpired      => studyProgramViewRepo.notExpired()
          case CurrentlyActive => studyProgramViewRepo.currentlyActive()
        }
        res.map(res => Ok(Json.toJson(res)))
      }
    }
}
