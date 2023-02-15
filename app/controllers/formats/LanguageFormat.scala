package controllers.formats

import models.core.Language
import play.api.libs.json.{Format, Json}

trait LanguageFormat {
  implicit val languageFormat: Format[Language] =
    Json.format[Language]
}
