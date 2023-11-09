package models

import play.api.libs.json.{Json, Writes}

sealed trait UniversityRole {
  def id: String
  def deLabel: String
  def enLabel: String
  override def toString = id
}

object UniversityRole {
  case object SGL extends UniversityRole {
    override def id: String = "sgl"
    override def deLabel: String = "Studiengangsleitung"
    override def enLabel: String = "Program Director"
  }
  case object PAV extends UniversityRole {
    override def id: String = "pav"
    override def deLabel: String = "Prüfungsausschussvorsitz"
    override def enLabel: String = "Exam Director"
  }

  def apply(id: String): UniversityRole =
    id.toLowerCase match {
      case "sgl" => SGL
      case "pav" => PAV
    }

  implicit def writes: Writes[UniversityRole] =
    r =>
      Json.obj(
        "id" -> r.id,
        "deLabel" -> r.deLabel,
        "enLabel" -> r.enLabel
      )
}
