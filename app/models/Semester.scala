package models

import models.core.AbbrevLabelLike
import play.api.libs.json.{Json, Writes}

trait Semester extends AbbrevLabelLike {
  def year: Int

  def id: String = s"${abbrev}_$year"
}

object Semester {
  def winter(_year: Int): Semester = new Semester {
    override def year: Int = _year

    override def abbrev: String = "wise"

    override def deLabel: String = "Wintersemester"

    override def enLabel: String = "Winter semester"
  }

  def summer(_year: Int): Semester = new Semester {
    override def year: Int = _year

    override def abbrev: String = "sose"

    override def deLabel: String = "Sommersemester"

    override def enLabel: String = "Summer semester"
  }

  def apply(id: String): Semester = {
    val Array(abbrev, year) = id.split("_")
    abbrev match {
      case "wise" => Semester.winter(year.toInt)
      case "sose" => Semester.summer(year.toInt)
    }
  }

  implicit def writes: Writes[Semester] =
    s =>
      Json.obj(
        "id" -> s.id,
        "year" -> s.year,
        "abbrev" -> s.abbrev,
        "deLabel" -> s.deLabel,
        "enLabel" -> s.enLabel
      )
}
