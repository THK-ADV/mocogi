package auth

import play.api.libs.json.Format

case class CampusId(value: String) extends AnyVal {
  override def toString = value

  def toMailAddress = value + "@th-koeln.de"
}

object CampusId {
  implicit def format: Format[CampusId] =
    Format.of[String].bimap(CampusId.apply, _.value)
}
