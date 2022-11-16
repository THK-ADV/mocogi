package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class PrerequisitesDbEntry(
    id: UUID,
    metadata: UUID,
    prerequisitesType: PrerequisiteType,
    text: String
)

final class PrerequisitesTable(tag: Tag)
    extends Table[PrerequisitesDbEntry](tag, "prerequisites") {

  def id = column[UUID]("id", O.PrimaryKey)

  def metadata = column[UUID]("metadata")

  def prerequisiteType =
    column[PrerequisiteType]("prerequisite_type")

  def text =
    column[String]("text")

  def metadataFk =
    foreignKey("metadata", metadata, TableQuery[MetadataTable])(_.id)

  override def * = (
    id,
    metadata,
    prerequisiteType,
    text
  ) <> (PrerequisitesDbEntry.tupled, PrerequisitesDbEntry.unapply)
}
