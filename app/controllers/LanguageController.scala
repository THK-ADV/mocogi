package controllers

import basedata.Language
import controllers.json.LanguageFormat
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
    with YamlController[Language, Language] {
  override implicit val writesOut: Writes[Language] = languageFormat
  override implicit val writesIn: Writes[Language] = languageFormat
}
