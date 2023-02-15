package controllers.core

import controllers.formats.AssessmentMethodFormat
import models.core.AssessmentMethod
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.AssessmentMethodService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class AssessmentMethodController @Inject() (
    cc: ControllerComponents,
    val service: AssessmentMethodService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AssessmentMethodFormat
    with SimpleYamlController[AssessmentMethod] {
  override implicit val writes: Writes[AssessmentMethod] =
    assessmentMethodFormat
}
