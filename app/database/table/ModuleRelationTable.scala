package database.table

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

  def moduleFk =
    foreignKey("module", module, TableQuery[MetadataTable])(_.id)

  def relationModuleFk =
    foreignKey("relation_module", relationModule, TableQuery[MetadataTable])(_.id)

  override def * = (
    module,
    relationType,
    relationModule
  ) <> (ModuleRelationDbEntry.tupled, ModuleRelationDbEntry.unapply)
}