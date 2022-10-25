package controllers

import basedata.Faculty
import controllers.json.FacultyFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FacultyController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with FacultyFormat
    with YamlController[Faculty, Faculty] {
  override val service = ???
  override implicit val writesOut: Writes[Faculty] = facultyFormat
  override implicit val writesIn: Writes[Faculty] = facultyFormat
}
