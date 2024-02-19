package models.core

import play.api.libs.json.Writes

sealed trait PersonStatus {
  def deLabel: String
  def enLabel: String
  def id: String = enLabel
  override def toString = id
}

object PersonStatus {
  case object Active extends PersonStatus {
    override def deLabel = "aktiv"
    override def enLabel = "active"
  }

  case object Inactive extends PersonStatus {
    override def deLabel = "inaktiv"
    override def enLabel = "inactive"
  }

  case object Unknown extends PersonStatus {
    override def deLabel = "unbekannt"
    override def enLabel = "unknown"
  }

  def apply(string: String): PersonStatus =
    string.toLowerCase match {
      case "active"   => Active
      case "inactive" => Inactive
      case _          => Unknown
    }

  implicit def writes: Writes[PersonStatus] =
    Writes.of[String].contramap(_.id)
}
