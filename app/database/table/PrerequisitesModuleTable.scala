package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

case class PrerequisitesModuleDbEntry(
    prerequisites: UUID,
    module: UUID
)

final class PrerequisitesModuleTable(tag: Tag) extends Table[PrerequisitesModuleDbEntry](tag, "prerequisites_module") {

  def prerequisites = column[UUID]("prerequisites", O.PrimaryKey)

  def module = column[UUID]("module", O.PrimaryKey)

  override def * = (
    prerequisites,
    module
  ) <> (PrerequisitesModuleDbEntry.apply, PrerequisitesModuleDbEntry.unapply)
}
