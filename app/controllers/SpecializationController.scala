package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.Specialization
import play.api.cache.Cached
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.SpecializationService

@Singleton
final class SpecializationController @Inject() (
    cc: ControllerComponents,
    val service: SpecializationService,
    val cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Specialization] {
  implicit override val writes: Writes[Specialization] = Specialization.writes
}
