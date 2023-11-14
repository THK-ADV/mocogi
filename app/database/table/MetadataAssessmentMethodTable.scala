package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class MetadataAssessmentMethodDbEntry(
    id: UUID,
    metadata: UUID,
    assessmentMethod: String,
    assessmentMethodType: AssessmentMethodType,
    percentage: Option[Double]
)

final class MetadataAssessmentMethodTable(tag: Tag)
    extends Table[MetadataAssessmentMethodDbEntry](
      tag,
      "metadata_assessment_method"
    ) {

  def id = column[UUID]("id", O.PrimaryKey)

  def metadata = column[UUID]("metadata")

  def assessmentMethod = column[String]("assessment_method")

  def assessmentMethodType =
    column[AssessmentMethodType]("assessment_method_type")

  def percentage = column[Option[Double]]("percentage")

  override def * = (
    id,
    metadata,
    assessmentMethod,
    assessmentMethodType,
    percentage
  ) <> (MetadataAssessmentMethodDbEntry.tupled, MetadataAssessmentMethodDbEntry.unapply)
}
