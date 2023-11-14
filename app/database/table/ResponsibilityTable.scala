package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ResponsibilityDbEntry(
    metadata: UUID,
    person: String,
    responsibilityType: ResponsibilityType
)

final class ResponsibilityTable(tag: Tag)
    extends Table[ResponsibilityDbEntry](tag, "responsibility") {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def person = column[String]("person", O.PrimaryKey)

  def isPerson(person: String) =
    this.person.toLowerCase === person.toLowerCase

  def responsibilityType =
    column[ResponsibilityType]("responsibility_type", O.PrimaryKey)

  def personFk =
    foreignKey("person", person, TableQuery[PersonTable])(_.id)

  override def * = (
    metadata,
    person,
    responsibilityType
  ) <> (ResponsibilityDbEntry.tupled, ResponsibilityDbEntry.unapply)
}
