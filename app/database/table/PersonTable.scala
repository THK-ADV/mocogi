package database.table

import parsing.types.Person
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
  ) <> (Person.tupled, Person.unapply)
}
