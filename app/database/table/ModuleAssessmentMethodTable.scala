package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api.*

case class ModuleAssessmentMethodDbEntry(
    id: UUID,
    module: UUID,
    assessmentMethod: String,
    percentage: Option[Double],
    precondition: Option[List[String]]
)

final class ModuleAssessmentMethodTable(tag: Tag)
    extends Table[ModuleAssessmentMethodDbEntry](tag, "module_assessment_method") {

  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  def id = column[UUID]("id", O.PrimaryKey)

  def module = column[UUID]("module")

  def assessmentMethod = column[String]("assessment_method")

  def percentage = column[Option[Double]]("percentage")

  def precondition = column[Option[List[String]]]("precondition")

  override def * = (
    id,
    module,
    assessmentMethod,
    percentage,
    precondition
  ) <> (ModuleAssessmentMethodDbEntry.apply, ModuleAssessmentMethodDbEntry.unapply)
}
