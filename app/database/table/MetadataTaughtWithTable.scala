package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class MetadataTaughtWithDbEntry(
    metadata: UUID,
    module: UUID
)

final class MetadataTaughtWithTable(tag: Tag)
    extends Table[MetadataTaughtWithDbEntry](tag, "metadata_taught_with") {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def module = column[UUID]("module", O.PrimaryKey)

  override def * = (
    metadata,
    module
  ) <> (MetadataTaughtWithDbEntry.tupled, MetadataTaughtWithDbEntry.unapply)
}
