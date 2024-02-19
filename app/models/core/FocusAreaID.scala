package models.core

import play.api.libs.json.Writes

case class FocusAreaID(id: String) extends AnyVal

object FocusAreaID {
  implicit def writes: Writes[FocusAreaID] =
    Writes.of[String].contramap(_.id)
}
