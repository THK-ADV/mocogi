package models.core

import play.api.libs.json.{Json, Writes}

trait AbbrevLabelLike {
  def abbrev: String
  def deLabel: String
  def enLabel: String
}

object AbbrevLabelLike {

  def apply(t: (String, String, String)): AbbrevLabelLike =
    new AbbrevLabelLike {
      override def abbrev: String = t._1
      override def deLabel: String = t._2
      override def enLabel: String = t._3
    }

  implicit val writes: Writes[AbbrevLabelLike] =
    o =>
      Json.obj(
        "abbrev" -> o.abbrev,
        "deLabel" -> o.deLabel,
        "enLabel" -> o.enLabel
      )
}
