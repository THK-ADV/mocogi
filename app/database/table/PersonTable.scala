package database.table

import basedata.{Faculty, Person}
import slick.jdbc.PostgresProfile.api._

final class PersonTable(tag: Tag) extends Table[Person](tag, "person") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def lastname = column[String]("lastname")

  def firstname = column[String]("firstname")

  def title = column[String]("title")

  def faculty = column[String]("faculty")

  override def * = (
    abbrev,
    lastname,
    firstname,
    title,
    faculty
  ) <> (mapRow, unmapRow)

  def mapRow: ((String, String, String, String, String)) => Person = {
    case (abbrev, lastname, firstname, title, faculty) =>
      Person(
        abbrev,
        lastname,
        firstname,
        title,
        Faculty(faculty, "", "")
      ) // TODO
  }

  def unmapRow: Person => Option[(String, String, String, String, String)] =
    a => Option((a.abbrev, a.lastname, a.firstname, a.title, a.faculty.abbrev))
}
