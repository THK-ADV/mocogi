package controllers

import basedata.Season
import controllers.json.SeasonFormat
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.SeasonService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class SeasonController @Inject() (
    cc: ControllerComponents,
    val service: SeasonService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SeasonFormat
    with YamlController[Season, Season] {
  override implicit val writesOut: Writes[Season] = seasonFormat
  override implicit val writesIn: Writes[Season] = seasonFormat
}
