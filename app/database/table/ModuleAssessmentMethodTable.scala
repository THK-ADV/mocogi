package database.table

import java.util.UUID

import models.AssessmentMethodType
import slick.jdbc.PostgresProfile.api.*

case class ModuleAssessmentMethodDbEntry(
    id: UUID,
    module: UUID,
    assessmentMethod: String,
    assessmentMethodType: AssessmentMethodType,
    percentage: Option[Double]
)

final class ModuleAssessmentMethodTable(tag: Tag)
    extends Table[ModuleAssessmentMethodDbEntry](
      tag,
      "module_assessment_method"
    ) {

  def id = column[UUID]("id", O.PrimaryKey)

  def module = column[UUID]("module")

  def assessmentMethod = column[String]("assessment_method")

  def assessmentMethodType =
    column[AssessmentMethodType]("assessment_method_type")

  def percentage = column[Option[Double]]("percentage")

  override def * = (
    id,
    module,
    assessmentMethod,
    assessmentMethodType,
    percentage
  ) <> (ModuleAssessmentMethodDbEntry.apply, ModuleAssessmentMethodDbEntry.unapply)
}
