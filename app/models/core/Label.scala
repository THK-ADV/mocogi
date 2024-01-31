package models.core

import play.api.libs.json.{Json, Writes}

trait Label {
  def deLabel: String
  def enLabel: String
}

object Label {
  implicit def writes: Writes[Label] =
    o =>
      Json.obj(
        "deLabel" -> o.deLabel,
        "enLabel" -> o.enLabel
      )
}
