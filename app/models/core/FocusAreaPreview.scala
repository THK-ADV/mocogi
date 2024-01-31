package models.core

import play.api.libs.json.Writes

case class FocusAreaPreview(id: String) extends AnyVal // TODO remove?

object FocusAreaPreview {
  implicit def writes: Writes[FocusAreaPreview] =
    Writes.of[String].contramap(_.id)
}
