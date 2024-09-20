package models.core

import play.api.libs.json.Json
import play.api.libs.json.OWrites

trait Label {
  def deLabel: String
  def enLabel: String
}

object Label {
  implicit def writes: OWrites[Label] =
    o =>
      Json.obj(
        "deLabel" -> o.deLabel,
        "enLabel" -> o.enLabel
      )
}
