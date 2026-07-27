package database.table

import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] case class ModuleRelationDbEntry(
    parent: UUID,
    child: UUID
)

private[database] final class ModuleRelationTable(tag: Tag)
    extends Table[ModuleRelationDbEntry](
      tag,
      Some(Schema.Modules.name),
      "module_relation"
    ) {

  def parent = column[UUID]("parent", O.PrimaryKey)

  def child = column[UUID]("child", O.PrimaryKey)

  override def * = (
    parent,
    child
  ) <> (ModuleRelationDbEntry.apply, ModuleRelationDbEntry.unapply)
}
