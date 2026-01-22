package database.table

import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] case class ModuleAssessmentMethodDbEntry(
    id: UUID,
    module: UUID,
    assessmentMethod: String,
    percentage: Option[Double],
    precondition: Option[List[String]]
)

private[database] final class ModuleAssessmentMethodTable(tag: Tag)
    extends Table[ModuleAssessmentMethodDbEntry](tag, Some(Schema.Modules.name), "module_assessment_method") {

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
