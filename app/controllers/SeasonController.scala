package controllers

import basedata.Season
import controllers.formats.SeasonFormat
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
    with SimpleYamlController[Season] {
  override implicit val writes: Writes[Season] = seasonFormat
}
