package models

import models.core.IDLabel
import play.api.libs.json.Writes

sealed trait UniversityRole extends IDLabel {
  override def toString = id
}

object UniversityRole {
  case object SGL extends UniversityRole {
    override def id: String      = "sgl"
    override def deLabel: String = "Studiengangsleitung"
    override def enLabel: String = "Program Director"
  }
  case object PAV extends UniversityRole {
    override def id: String      = "pav"
    override def deLabel: String = "Prüfungsausschussvorsitz"
    override def enLabel: String = "Exam Director"
  }

  def apply(id: String): UniversityRole =
    id.toLowerCase match {
      case "sgl" => SGL
      case "pav" => PAV
    }

  implicit def writes: Writes[UniversityRole] =
    Writes.of[IDLabel].contramap(identity)

  def all() = Set(SGL, PAV)
}
