package controllers.core

import controllers.formats.GradesFormat
import models.core.Grade
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.GradeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GradeController @Inject() (
    cc: ControllerComponents,
    val service: GradeService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with GradesFormat
    with SimpleYamlController[Grade] {
  override implicit val writes: Writes[Grade] = gradesFormat
}