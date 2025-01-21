package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.Degree
import play.api.cache.Cached
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.DegreeService

@Singleton
final class DegreeController @Inject() (
    cc: ControllerComponents,
    val service: DegreeService,
    val cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Degree] {
  implicit override val writes: Writes[Degree] = Degree.writes
}
