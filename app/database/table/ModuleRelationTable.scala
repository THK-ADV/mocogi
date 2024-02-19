package database.table

import models.ModuleRelationType
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ModuleRelationDbEntry(
    module: UUID,
    relationType: ModuleRelationType,
    relationModule: UUID
)

final class ModuleRelationTable(tag: Tag)
    extends Table[ModuleRelationDbEntry](
      tag,
      "module_relation"
    ) {

  def module = column[UUID]("module", O.PrimaryKey)

  def relationType = column[ModuleRelationType]("relation_type", O.PrimaryKey)

  def relationModule = column[UUID]("relation_module", O.PrimaryKey)

  override def * = (
    module,
    relationType,
    relationModule
  ) <> (ModuleRelationDbEntry.tupled, ModuleRelationDbEntry.unapply)
}
