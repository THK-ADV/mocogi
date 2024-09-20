package catalog

import models.core.Label
import play.api.libs.json.Json
import play.api.libs.json.Writes

trait Semester extends Label {
  def id: String = s"${abbrev}_$year"
  def year: Int
  def abbrev: String
}

object Semester {
  def winter(_year: Int): Semester = new Semester {
    override def year: Int = _year

    def abbrev: String = "wise"

    def deLabel: String = "Wintersemester"

    def enLabel: String = "Winter semester"
  }

  def summer(_year: Int): Semester = new Semester {
    override def year: Int = _year

    def abbrev: String = "sose"

    def deLabel: String = "Sommersemester"

    def enLabel: String = "Summer semester"
  }

  def apply(id: String): Semester = {
    val Array(abbrev, year) = id.split("_")
    abbrev match {
      case "wise" => winter(year.toInt)
      case "sose" => summer(year.toInt)
    }
  }

  implicit def writes: Writes[Semester] =
    s =>
      Json.obj(
        "id"      -> s.id,
        "abbrev"  -> s.abbrev,
        "year"    -> s.year,
        "deLabel" -> s.deLabel,
        "enLabel" -> s.enLabel
      )
}
