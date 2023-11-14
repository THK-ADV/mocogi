package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class MetadataGlobalCriteriaDbEntry(
    metadata: UUID,
    globalCriteria: String
)

final class MetadataGlobalCriteriaTable(tag: Tag)
    extends Table[MetadataGlobalCriteriaDbEntry](
      tag,
      "metadata_global_criteria"
    ) {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def globalCriteria = column[String]("global_criteria", O.PrimaryKey)

  override def * = (
    metadata,
    globalCriteria
  ) <> (MetadataGlobalCriteriaDbEntry.tupled, MetadataGlobalCriteriaDbEntry.unapply)
}
