package controllers

import models.core.Specialization
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.SpecializationService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class SpecializationController @Inject() (
    cc: ControllerComponents,
    val service: SpecializationService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SimpleYamlController[Specialization] {
  override implicit val writes: Writes[Specialization] = Specialization.writes
}
