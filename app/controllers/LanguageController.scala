package controllers

import models.core.ModuleLanguage
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
    with YamlController[ModuleLanguage] {
  override implicit val writes: Writes[ModuleLanguage] = ModuleLanguage.writes
}
