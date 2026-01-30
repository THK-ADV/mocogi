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

@Singleton
final class SemesterPlanController @Inject() (
    cc: ControllerComponents,
    jsonRepository: JSONRepository,
    holidayService: HolidayService,
    cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def allByNow() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async(_ => jsonRepository.allByNow().map(Ok(_)))
    }

  def holidays() =
    cached.status(r => r.method + r.uri, 200, 1.hour) {
      Action.async(_ => holidayService.holidaysByNow().map(js => Ok(Json.toJson(js))))
    }
}
