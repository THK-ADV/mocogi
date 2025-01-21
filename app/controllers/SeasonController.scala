package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.Season
import play.api.cache.Cached
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.SeasonService

@Singleton
final class SeasonController @Inject() (
    cc: ControllerComponents,
    val service: SeasonService,
    val cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[Season] {
  implicit override val writes: Writes[Season] = Season.writes
}
