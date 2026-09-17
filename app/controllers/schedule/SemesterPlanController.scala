package controllers.schedule

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try

import database.repo.JSONRepository
import models.Semester
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
      Action.async { request =>
        val range = Try {
          request.getQueryString("semester") match {
            case Some(id) =>
              val (from, to) = Semester.dateRange(id)
              (from.toLocalDate, to.toLocalDate)
            case None =>
              val today = LocalDate.now(ZoneId.of("Europe/Berlin"))
              (Semester.of(today).start, Semester.next(today).end)
          }
        }
        range.toEither match {
          case Right((from, to)) => jsonRepository.semesterPlan(from, to).map(Ok(_))
          case Left(_)           => Future.successful(BadRequest("Invalid semester"))
        }
      }
    }

  def holidays() =
    cached.unlessNoCache(r => r.method + r.uri, 200, 1.hour) {
      Action.async(_ => holidayService.holidaysByNow().map(js => Ok(Json.toJson(js))))
    }
}
