package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.FocusArea
import play.api.cache.Cached
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.FocusAreaService

@Singleton
final class FocusAreaController @Inject() (
    cc: ControllerComponents,
    val service: FocusAreaService,
    val cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[FocusArea] {
  implicit override val writes: Writes[FocusArea] = FocusArea.writes
}
