package database.table

import slick.jdbc.PostgresProfile.api._

case class PersonInFaculty(person: String, faculty: String)

final class PersonInFacultyTable(tag: Tag)
    extends Table[PersonInFaculty](tag, "person_in_faculty") {

  def person = column[String]("person", O.PrimaryKey)

  def faculty = column[String]("faculty", O.PrimaryKey)

  def personFk =
    foreignKey("person", person, TableQuery[PersonTable])(_.id)

  def facultyFk =
    foreignKey("faculty", faculty, TableQuery[FacultyTable])(_.abbrev)

  override def * = (
    person,
    faculty
  ) <> (PersonInFaculty.tupled, PersonInFaculty.unapply)
}
