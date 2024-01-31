package models.core

import play.api.libs.json.{Json, Writes}

case class Language(id: String, deLabel: String, enLabel: String)
    extends IDLabel

object Language {
  implicit def writes: Writes[Language] = Json.writes
}
