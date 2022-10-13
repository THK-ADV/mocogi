package controllers

import basedata.{Faculty, FacultyFormat}
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
    with YamlController[Faculty] {
  override implicit val writes: Writes[Faculty] = facultyFormat
  override val service = ???
}
