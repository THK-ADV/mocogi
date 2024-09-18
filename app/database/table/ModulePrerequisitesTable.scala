package database.table

import models.PrerequisiteType
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ModulePrerequisitesDbEntry(
    id: UUID,
    module: UUID,
    prerequisitesType: PrerequisiteType,
    text: String
)

final class ModulePrerequisitesTable(tag: Tag)
    extends Table[ModulePrerequisitesDbEntry](tag, "module_prerequisites") {

  def id = column[UUID]("id", O.PrimaryKey)

  def module = column[UUID]("module")

  def prerequisiteType =
    column[PrerequisiteType]("prerequisite_type")

  def text =
    column[String]("text")

  override def * = (
    id,
    module,
    prerequisiteType,
    text
  ) <> (ModulePrerequisitesDbEntry.apply, ModulePrerequisitesDbEntry.unapply)
}
