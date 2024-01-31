package database.table

import models.ResponsibilityType
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ResponsibilityDbEntry(
    metadata: UUID,
    identity: String,
    responsibilityType: ResponsibilityType
)

final class ResponsibilityTable(tag: Tag)
    extends Table[ResponsibilityDbEntry](tag, "responsibility") {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

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
    metadata,
    identity,
    responsibilityType
  ) <> (ResponsibilityDbEntry.tupled, ResponsibilityDbEntry.unapply)
}
