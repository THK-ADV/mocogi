package controllers

import basedata.AssessmentMethod
import controllers.json.AssessmentMethodFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.AssessmentMethodService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class AssessmentMethodController @Inject() (
    cc: ControllerComponents,
    val service: AssessmentMethodService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AssessmentMethodFormat
    with YamlController[AssessmentMethod] {
  override implicit val writes: Writes[AssessmentMethod] =
    assessmentMethodFormat
}
