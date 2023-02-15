package database.table

import models.core.PersonStatus
import slick.jdbc.PostgresProfile.api._

case class PersonDbEntry(
    id: String,
    lastname: String,
    firstname: String,
    title: String,
    faculties: List[String],
    abbreviation: String,
    status: PersonStatus,
    kind: String
)

final class PersonTable(tag: Tag) extends Table[PersonDbEntry](tag, "person") {

  def id = column[String]("id", O.PrimaryKey)

  def lastname = column[String]("lastname")

  def firstname = column[String]("firstname")

  def title = column[String]("title")

  def abbreviation = column[String]("abbreviation")

  def status = column[String]("status")

  def kind = column[String]("kind")

  override def * = (
    id,
    lastname,
    firstname,
    title,
    abbreviation,
    status,
    kind
  ) <> (mapRow, unmapRow)

  def mapRow: (
      (String, String, String, String, String, String, String)
  ) => PersonDbEntry = {
    case (
          id,
          lastname,
          firstname,
          title,
          abbreviation,
          status,
          kind
        ) =>
      PersonDbEntry(
        id,
        lastname,
        firstname,
        title,
        Nil,
        abbreviation,
        PersonStatus(status),
        kind
      )
  }

  def unmapRow: PersonDbEntry => Option[
    (String, String, String, String, String, String, String)
  ] = { a =>
    Option(
      (
        a.id,
        a.lastname,
        a.firstname,
        a.title,
        a.abbreviation,
        a.status.toString,
        a.kind
      )
    )
  }
}
