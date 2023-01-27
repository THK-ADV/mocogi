package controllers.formats

import basedata.Language
import play.api.libs.json.{Format, Json}

trait LanguageFormat {
  implicit val languageFormat: Format[Language] =
    Json.format[Language]
}
