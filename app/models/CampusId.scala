package models

import play.api.libs.json.Format

case class CampusId(value: String) extends AnyVal

object CampusId {
  implicit def format: Format[CampusId] =
    Format.of[String].bimap(CampusId.apply, _.value)
}