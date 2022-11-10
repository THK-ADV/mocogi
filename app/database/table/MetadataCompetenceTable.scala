package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class MetadataCompetenceDbEntry(
    metadata: UUID,
    competence: String
)

final class MetadataCompetenceTable(tag: Tag)
    extends Table[MetadataCompetenceDbEntry](tag, "metadata_competence") {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def competence = column[String]("competence", O.PrimaryKey)

  def metadataFk =
    foreignKey("metadata", metadata, TableQuery[MetadataTable])(_.id)

  def competenceFk =
    foreignKey("competence", competence, TableQuery[CompetenceTable])(_.abbrev)

  override def * = (
    metadata,
    competence
  ) <> (MetadataCompetenceDbEntry.tupled, MetadataCompetenceDbEntry.unapply)
}
