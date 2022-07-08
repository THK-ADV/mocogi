package controllers

import controllers.json.SeasonFormat
import parsing.types.Season
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
    with YamlController[Season] {
  override implicit val writes: Writes[Season] = seasonFormat
}
