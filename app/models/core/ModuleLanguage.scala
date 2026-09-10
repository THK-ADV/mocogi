package models.core

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleLanguage(id: String, deLabel: String, enLabel: String) extends IDLabel

object ModuleLanguage {
  given Format[ModuleLanguage] = Json.format

  def isGerman(id: String): Boolean =
    id == "de"

  def isEnglish(id: String): Boolean =
    id == "en"
}
