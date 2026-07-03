package database.table

import java.util.UUID

import database.MyPostgresProfile.api.*
import database.Schema
import models.ModulePermittedAssessmentMethod
import slick.lifted.ProvenShape

private[database] final class ModulePermittedAssessmentMethodTable(tag: Tag)
    extends Table[ModulePermittedAssessmentMethod](
      tag,
      Some(Schema.Modules.name),
      "permitted_assessment_method_for_module"
    ) {

  import database.MyPostgresProfile.MyAPI.simpleArrayColumnExtensionMethods
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  def module            = column[UUID]("module", O.PrimaryKey)
  def assessmentMethods = column[List[String]]("assessment_methods")

  def permittedMethodIdsUnnest() = assessmentMethods.unnest()

  override def * : ProvenShape[ModulePermittedAssessmentMethod] = (module, assessmentMethods) <> (
    ModulePermittedAssessmentMethod.apply,
    ModulePermittedAssessmentMethod.unapply
  )
}
