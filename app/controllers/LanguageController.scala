package controllers

import models.core.Language
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.core.LanguageService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class LanguageController @Inject() (
    cc: ControllerComponents,
    val service: LanguageService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with SimpleYamlController[Language] {
  override implicit val writes: Writes[Language] = Language.writes
}
