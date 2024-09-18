package database.table

import slick.jdbc.PostgresProfile.api.*

import java.util.UUID

case class ModuleAssessmentMethodPreconditionDbEntry(
    assessmentMethod: String,
    moduleAssessmentMethod: UUID
)

final class ModuleAssessmentMethodPreconditionTable(tag: Tag)
    extends Table[ModuleAssessmentMethodPreconditionDbEntry](
      tag,
      "module_assessment_method_precondition"
    ) {

  def assessmentMethod = column[String]("assessment_method", O.PrimaryKey)

  def moduleAssessmentMethod =
    column[UUID]("module_assessment_method", O.PrimaryKey)

  override def * = (
    assessmentMethod,
    moduleAssessmentMethod
  ) <> (
    ModuleAssessmentMethodPreconditionDbEntry.apply,
    ModuleAssessmentMethodPreconditionDbEntry.unapply
  )
}
