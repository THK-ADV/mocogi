package controllers

import basedata.Grade
import controllers.json.GradesFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GradeController @Inject() (
    cc: ControllerComponents,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with GradesFormat
    with YamlController[Grade] {
  override implicit val writes: Writes[Grade] = gradesFormat
  override val service = ???
}
