package controllers

import controllers.json.LanguageFormat
import parsing.types.Language
import play.api.libs.json.Writes
import play.api.mvc.{AbstractController, ControllerComponents}
import service.LanguageService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class LanguageController @Inject() (
    cc: ControllerComponents,
    val service: LanguageService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with LanguageFormat
    with YamlController[Language] {
  override implicit val writes: Writes[Language] = languageFormat
}
