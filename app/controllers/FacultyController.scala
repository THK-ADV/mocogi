package controllers

import models.core.Faculty
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.FacultyService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FacultyController @Inject() (
    cc: ControllerComponents,
    val service: FacultyService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Faculty] {
  override implicit val writes: Writes[Faculty] = Faculty.writes
}
