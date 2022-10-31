package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class AssessmentMethodMetadataPreconditionDbEntry(
    assessmentMethodMetadata: UUID,
    assessmentMethod: String
)

final class AssessmentMethodMetadataPreconditionTable(tag: Tag)
    extends Table[AssessmentMethodMetadataPreconditionDbEntry](
      tag,
      "assessment_method_metadata_precondition"
    ) {

  def assessmentMethodMetadata =
    column[UUID]("assessment_method_metadata", O.PrimaryKey)

  def assessmentMethod = column[String]("assessment_method")

  override def * = (
    assessmentMethodMetadata,
    assessmentMethod
  ) <> (AssessmentMethodMetadataPreconditionDbEntry.tupled, AssessmentMethodMetadataPreconditionDbEntry.unapply)
}
