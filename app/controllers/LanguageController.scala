package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import models.core.ModuleLanguage
import play.api.cache.Cached
import play.api.libs.json.Writes
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import service.core.LanguageService

@Singleton
final class LanguageController @Inject() (
    cc: ControllerComponents,
    val service: LanguageService,
    val cached: Cached,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with YamlController[ModuleLanguage] {
  implicit override val writes: Writes[ModuleLanguage] = ModuleLanguage.writes
}
