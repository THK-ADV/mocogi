package database.table

import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*
import database.table.core.AssessmentMethodTable

private[database] case class ModuleUsedAssessmentMethodDbEntry(
    id: UUID,
    module: UUID,
    assessmentMethod: String,
    percentage: Option[Double],
    precondition: Option[List[String]]
)

private[database] final class ModuleUsedAssessmentMethodTable(tag: Tag)
    extends Table[ModuleUsedAssessmentMethodDbEntry](tag, Some(Schema.Modules.name), "module_assessment_method") {

  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  def id = column[UUID]("id", O.PrimaryKey)

  def module = column[UUID]("module")

  def assessmentMethod = column[String]("assessment_method")

  def percentage = column[Option[Double]]("percentage")

  def precondition = column[Option[List[String]]]("precondition")

  def assessmentMethodFk = foreignKey("foreignKey", assessmentMethod, TableQuery[AssessmentMethodTable])(_.id)

  override def * = (
    id,
    module,
    assessmentMethod,
    percentage,
    precondition
  ) <> (ModuleUsedAssessmentMethodDbEntry.apply, ModuleUsedAssessmentMethodDbEntry.unapply)
}
