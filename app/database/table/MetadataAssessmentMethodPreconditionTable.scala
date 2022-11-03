package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class MetadataAssessmentMethodPreconditionDbEntry(
    assessmentMethod: String,
    metadataAssessmentMethod: UUID
)

final class MetadataAssessmentMethodPreconditionTable(tag: Tag)
    extends Table[MetadataAssessmentMethodPreconditionDbEntry](
      tag,
      "metadata_assessment_method_precondition"
    ) {

  def assessmentMethod = column[String]("assessment_method", O.PrimaryKey)

  def metadataAssessmentMethod =
    column[UUID]("metadata_assessment_method", O.PrimaryKey)

  override def * = (
    assessmentMethod,
    metadataAssessmentMethod
  ) <> (MetadataAssessmentMethodPreconditionDbEntry.tupled, MetadataAssessmentMethodPreconditionDbEntry.unapply)
}
