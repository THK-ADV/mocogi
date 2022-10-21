package database.table

import database.entities.PersonDbEntry
import slick.jdbc.PostgresProfile.api._

final class PersonTable(tag: Tag) extends Table[PersonDbEntry](tag, "person") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def lastname = column[String]("lastname")

  def firstname = column[String]("firstname")

  def title = column[String]("title")

  def faculty = column[String]("faculty")

  def facultyFk =
    foreignKey("faculty", faculty, TableQuery[FacultyTable])(_.abbrev)

  override def * = (
    abbrev,
    lastname,
    firstname,
    title,
    faculty
  ) <> (PersonDbEntry.tupled, PersonDbEntry.unapply)
}
