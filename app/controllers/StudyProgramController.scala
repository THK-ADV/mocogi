package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import database.view.StudyProgramViewRepository
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.StudyProgramService

@Singleton
final class StudyProgramController @Inject() (
    cc: ControllerComponents,
    studyProgramViewRepo: StudyProgramViewRepository,
    studyProgramService: StudyProgramService,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def all() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async { request =>
        if request.isExtended then studyProgramViewRepo.all().map(res => Ok(Json.toJson(res)))
        else studyProgramService.all().map(res => Ok(Json.toJson(res)))
      }
    }
}
