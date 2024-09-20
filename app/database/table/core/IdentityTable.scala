package database.table.core

import models.core.PersonStatus
import slick.jdbc.PostgresProfile.api._

case class IdentityDbEntry(
    id: String,
    lastname: String,
    firstname: String,
    title: String,
    faculties: List[String],
    abbreviation: String,
    campusId: Option[String],
    status: PersonStatus,
    kind: String
)

final class IdentityTable(tag: Tag) extends Table[IdentityDbEntry](tag, "identity") {

  def id = column[String]("id", O.PrimaryKey)

  def lastname = column[String]("lastname")

  def firstname = column[String]("firstname")

  def title = column[String]("title")

  def abbreviation = column[String]("abbreviation")

  def status = column[String]("status")

  def kind = column[String]("kind")

  def campusId = column[Option[String]]("campus_id")

  override def * = (
    id,
    lastname,
    firstname,
    title,
    abbreviation,
    status,
    kind,
    campusId
  ) <> (mapRow, unmapRow)

  def mapRow: (
      (String, String, String, String, String, String, String, Option[String])
  ) => IdentityDbEntry = {
    case (
          id,
          lastname,
          firstname,
          title,
          abbreviation,
          status,
          kind,
          campusId
        ) =>
      IdentityDbEntry(
        id,
        lastname,
        firstname,
        title,
        Nil,
        abbreviation,
        campusId,
        PersonStatus(status),
        kind
      )
  }

  def unmapRow: IdentityDbEntry => Option[
    (String, String, String, String, String, String, String, Option[String])
  ] = { a =>
    Option(
      (
        a.id,
        a.lastname,
        a.firstname,
        a.title,
        a.abbreviation,
        a.status.toString,
        a.kind,
        a.campusId
      )
    )
  }
}
