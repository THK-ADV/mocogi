package database.table

import java.util.UUID

import database.Schema
import models.ResponsibilityType
import slick.jdbc.PostgresProfile.api.*

private[database] case class ModuleResponsibilityDbEntry(
    module: UUID,
    identity: String,
    responsibilityType: ResponsibilityType
)

private[database] final class ModuleResponsibilityTable(tag: Tag)
    extends Table[ModuleResponsibilityDbEntry](tag, Some(Schema.Modules.name), "module_responsibility") {

  def module = column[UUID]("module", O.PrimaryKey)

  def identity = column[String]("identity", O.PrimaryKey)

  def responsibilityType =
    column[ResponsibilityType]("responsibility_type", O.PrimaryKey)

  def isModuleManager = {
    val moduleManger: ResponsibilityType = ResponsibilityType.ModuleManagement
    this.responsibilityType === moduleManger
  }

  def isIdentity(identity: String) =
    this.identity.toLowerCase === identity.toLowerCase

  override def * = (
    module,
    identity,
    responsibilityType
  ) <> (ModuleResponsibilityDbEntry.apply, ModuleResponsibilityDbEntry.unapply)
}
