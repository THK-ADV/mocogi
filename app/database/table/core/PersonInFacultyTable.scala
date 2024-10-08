package database.table.core

import slick.jdbc.PostgresProfile.api._

case class PersonInFaculty(person: String, faculty: String)

final class PersonInFacultyTable(tag: Tag) extends Table[PersonInFaculty](tag, "person_in_faculty") {

  def person = column[String]("person", O.PrimaryKey)

  def faculty = column[String]("faculty", O.PrimaryKey)

  override def * = (
    person,
    faculty
  ) <> (PersonInFaculty.apply, PersonInFaculty.unapply)
}
