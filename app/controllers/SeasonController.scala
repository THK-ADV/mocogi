package controllers

import models.core.Season
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.SeasonService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class SeasonController @Inject() (
    cc: ControllerComponents,
    val service: SeasonService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SimpleYamlController[Season] {
  override implicit val writes: Writes[Season] = Season.writes
}
