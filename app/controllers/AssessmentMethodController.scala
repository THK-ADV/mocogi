package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.AssessmentMethod
import play.api.cache.Cached
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.AssessmentMethodService

@Singleton
final class AssessmentMethodController @Inject() (
    cc: ControllerComponents,
    val service: AssessmentMethodService,
    val cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[AssessmentMethod] {
  implicit override val writes: Writes[AssessmentMethod] =
    AssessmentMethod.writes
}
