package models

import java.time.LocalDate
import java.time.Month

import models.core.Label
import play.api.libs.json.Json
import play.api.libs.json.Writes

trait Semester extends Label {
  def id: String = s"${abbrev}_$year"
  def year: Int
  def abbrev: String
}

object Semester {

  given Ordering[Semester] = (lhs, rhs) => {
    val yearRes = lhs.year.compareTo(rhs.year)
    if yearRes == 0 then {
      // wise is always the latest semester if the year is the same
      (lhs.abbrev, rhs.abbrev) match {
        case ("wise", "sose") => 1
        case ("sose", "wise") => -1
        case _                => 0
      }
    } else yearRes
  }

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

  private def soSeStart = Month.MARCH.getValue
  private def soSeEnd   = Month.AUGUST.getValue

  def apply(id: String): Semester = {
    val Array(abbrev, year) = id.split("_")
    abbrev match {
      case "wise" => winter(year.toInt)
      case "sose" => summer(year.toInt)
    }
  }

  def current(date: LocalDate = LocalDate.now): Semester = {
    val month = date.getMonth.getValue
    if month >= soSeStart && month <= soSeEnd then Semester.summer(date.getYear)
    else Semester.winter(date.getYear)
  }

  def currentAndNext(date: LocalDate = LocalDate.now): List[Semester] = {
    val month = date.getMonth.getValue
    // 03. - 08.
    if month >= soSeStart && month <= soSeEnd
    then List(summer(date.getYear), winter(date.getYear))
    // 08. - 12.
    else if month >= soSeEnd && month <= 12 then List(winter(date.getYear), summer(date.getYear + 1))
    // 01. - 03.
    else List(winter(date.getYear - 1), summer(date.getYear))
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
