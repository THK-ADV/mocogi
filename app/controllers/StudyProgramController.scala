package controllers

import database.view.StudyProgramViewRepository
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.StudyProgramService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StudyProgramController @Inject() (
    cc: ControllerComponents,
    val service: StudyProgramService,
    val materializedView: StudyProgramViewRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {
  def all() =
    Action.async { _ =>
      service.all().map(xs => Ok(Json.toJson(xs)))
    }

  def allFromView() =
    Action.async { _ =>
      materializedView.all().map(res => Ok(Json.toJson(res)))
    }
}
