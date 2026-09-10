package controllers.schedule

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import database.repo.JSONRepository
import play.api.cache.Cached
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.schedule.HolidayService
import controllers.unlessNoCache
import controllers.ResourceCache

@Singleton
final class SemesterPlanController @Inject() (
    cc: ControllerComponents,
    jsonRepository: JSONRepository,
    holidayService: HolidayService,
    cached: Cached,
    cache: ResourceCache,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def allByNow() =
    cache("semesterplan", 1.hour) {
      Action.async(_ => jsonRepository.allByNow().map(Ok(_)))
    }

  def holidays() =
    cached.unlessNoCache(r => r.method + r.uri, 200, 1.hour) {
      Action.async(_ => holidayService.holidaysByNow().map(js => Ok(Json.toJson(js))))
    }
}
