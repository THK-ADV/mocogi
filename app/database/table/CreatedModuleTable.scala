package database.table

import java.util.UUID

import database.MyPostgresProfile.api.*
import models.CreatedModule
import slick.lifted.ProvenShape

final class CreatedModuleTable(tag: Tag) extends Table[CreatedModule](tag, "created_module_in_draft") {

  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  def module             = column[UUID]("module", O.PrimaryKey)
  def moduleTitle        = column[String]("module_title")
  def moduleAbbrev       = column[String]("module_abbrev")
  def moduleManagement   = column[List[String]]("module_management")
  def moduleECTS         = column[Double]("module_ects")
  def moduleType         = column[String]("module_type")
  def moduleMandatoryPOs = column[List[String]]("module_mandatory_pos")
  def moduleOptionalPOs  = column[List[String]]("module_optional_pos")

  override def * : ProvenShape[CreatedModule] =
    (
      module,
      moduleTitle,
      moduleAbbrev,
      moduleManagement,
      moduleECTS,
      moduleType,
      moduleMandatoryPOs,
      moduleOptionalPOs
    ) <> (
      CreatedModule.apply,
      CreatedModule.unapply
    )
}
