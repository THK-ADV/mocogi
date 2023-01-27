package controllers

import basedata.Faculty
import controllers.formats.FacultyFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.FacultyService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FacultyController @Inject() (
    cc: ControllerComponents,
    val service: FacultyService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with FacultyFormat
    with SimpleYamlController[Faculty] {
  override implicit val writes: Writes[Faculty] = facultyFormat
}
