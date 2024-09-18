package database.table

import models.ResponsibilityType
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ModuleResponsibilityDbEntry(
    module: UUID,
    identity: String,
    responsibilityType: ResponsibilityType
)

final class ModuleResponsibilityTable(tag: Tag)
    extends Table[ModuleResponsibilityDbEntry](tag, "module_responsibility") {

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
