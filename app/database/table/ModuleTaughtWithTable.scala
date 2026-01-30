package database.table

import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] case class ModuleTaughtWithDbEntry(
    module: UUID,
    moduleTaught: UUID
)

private[database] final class ModuleTaughtWithTable(tag: Tag)
    extends Table[ModuleTaughtWithDbEntry](tag, Some(Schema.Modules.name), "module_taught_with") {

  def module = column[UUID]("module", O.PrimaryKey)

  def moduleTaught = column[UUID]("module_taught", O.PrimaryKey)

  override def * = (
    module,
    moduleTaught
  ) <> (ModuleTaughtWithDbEntry.apply, ModuleTaughtWithDbEntry.unapply)
}
