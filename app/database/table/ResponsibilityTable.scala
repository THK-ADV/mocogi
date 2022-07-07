package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ResponsibilityTable(tag: Tag)
    extends Table[ResponsibilityDbEntry](tag, "responsibility") {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def person = column[String]("person", O.PrimaryKey)

  def kind = column[String]("kind", O.PrimaryKey)

  def metadataFk =
    foreignKey("metadata", metadata, TableQuery[MetadataTable])(_.id)

  def personFk =
    foreignKey("person", person, TableQuery[PersonTable])(_.abbrev)

  override def * = (
    metadata,
    person,
    kind
  ) <> (mapRow, unmapRow)

  def mapRow: ((UUID, String, String)) => ResponsibilityDbEntry = {
    case (metadata, person, kind) => ResponsibilityDbEntry(metadata, person, ResponsibilityType(kind))
  }

  def unmapRow: ResponsibilityDbEntry => Option[(UUID, String, String)] = { a =>
    Option((a.metadata, a.person, a.kind.toString))
  }
}
