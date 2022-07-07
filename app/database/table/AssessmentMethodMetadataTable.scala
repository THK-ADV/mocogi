package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class AssessmentMethodMetadataTable(tag: Tag)
    extends Table[AssessmentMethodMetadataDbEntry](
      tag,
      "assessment_method_metadata"
    ) {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def assessmentMethod = column[String]("assessment_method", O.PrimaryKey)

  def percentage = column[Option[Double]]("percentage")

  def metadataFk =
    foreignKey("metadata", metadata, TableQuery[MetadataTable])(_.id)

  def assessmentMethodFk =
    foreignKey(
      "assessment_method",
      assessmentMethod,
      TableQuery[AssessmentMethodTable]
    )(_.abbrev)

  override def * = (
    metadata,
    assessmentMethod,
    percentage
  ) <> (AssessmentMethodMetadataDbEntry.tupled, AssessmentMethodMetadataDbEntry.unapply)
}
