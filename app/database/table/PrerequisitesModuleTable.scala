package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class PrerequisitesModuleDbEntry(
    prerequisites: UUID,
    module: UUID
)

final class PrerequisitesModuleTable(tag: Tag)
    extends Table[PrerequisitesModuleDbEntry](tag, "prerequisites_module") {

  def prerequisites = column[UUID]("prerequisites", O.PrimaryKey)

  def module = column[UUID]("module", O.PrimaryKey)

  def moduleFk =
    foreignKey("module", module, TableQuery[ModuleCompendiumTable])(_.id)

  def prerequisitesFk =
    foreignKey("prerequisites", prerequisites, TableQuery[PrerequisitesTable])(
      _.id
    )

  override def * = (
    prerequisites,
    module
  ) <> (PrerequisitesModuleDbEntry.tupled, PrerequisitesModuleDbEntry.unapply)
}
