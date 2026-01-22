package database.table

import java.util.UUID

import database.Schema
import models.ModuleRelationType
import slick.jdbc.PostgresProfile.api.*

private[database] case class ModuleRelationDbEntry(
    module: UUID,
    relationType: ModuleRelationType,
    relationModule: UUID
)

private[database] final class ModuleRelationTable(tag: Tag)
    extends Table[ModuleRelationDbEntry](
      tag,
      Some(Schema.Modules.name),
      "module_relation"
    ) {

  def module = column[UUID]("module", O.PrimaryKey)

  def relationType = column[ModuleRelationType]("relation_type", O.PrimaryKey)

  def relationModule = column[UUID]("relation_module", O.PrimaryKey)

  override def * = (
    module,
    relationType,
    relationModule
  ) <> (ModuleRelationDbEntry.apply, ModuleRelationDbEntry.unapply)
}
