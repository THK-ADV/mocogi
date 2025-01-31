package database.table

import java.util.UUID

import database.MyPostgresProfile.api.*
import slick.lifted.ProvenShape

case class PermittedAssessmentMethodForModule(module: UUID, assessmentMethods: List[String])

final class PermittedAssessmentMethodForModuleTable(tag: Tag)
    extends Table[PermittedAssessmentMethodForModule](tag, "permitted_assessment_method_for_module") {

  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleArrayColumnExtensionMethods

  def module            = column[UUID]("module", O.PrimaryKey)
  def assessmentMethods = column[List[String]]("assessment_methods")
  
  def assessmentMethodsUnnest() = assessmentMethods.unnest()
  
  override def * : ProvenShape[PermittedAssessmentMethodForModule] = (module, assessmentMethods) <> (
    PermittedAssessmentMethodForModule.apply,
    PermittedAssessmentMethodForModule.unapply
  )
}
