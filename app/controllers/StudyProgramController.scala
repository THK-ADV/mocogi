package controllers

import database.view.StudyProgramViewRepository
import models.core.StudyProgram
import play.api.libs.json.{Json, Writes}
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
) extends AbstractController(cc)
    with YamlController[StudyProgram] {

  override implicit val writes: Writes[StudyProgram] = StudyProgram.writes

  // TODO how should the resource look like?
  def allFromView() =
    Action.async { _ =>
      materializedView.all().map(res => Ok(Json.toJson(res)))
    }
}
