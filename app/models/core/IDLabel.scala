package models.core

import play.api.libs.json.{Json, Writes}

trait IDLabel extends Label {
  def id: String
}

object IDLabel {

  def apply(t: (String, String, String)): IDLabel =
    new IDLabel {
      override def id: String = t._1
      override def deLabel: String = t._2
      override def enLabel: String = t._3
    }

  def apply(_id: String, _deLabel: String, _enLabel: String): IDLabel =
    new IDLabel {
      override def id: String = _id
      override def deLabel: String = _deLabel
      override def enLabel: String = _enLabel
    }

  implicit def writes: Writes[IDLabel] =
    o =>
      Json.obj(
        "id" -> o.id,
        "deLabel" -> o.deLabel,
        "enLabel" -> o.enLabel
      )
}
