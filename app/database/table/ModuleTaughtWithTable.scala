package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ModuleTaughtWithDbEntry(
    module: UUID,
    moduleTaught: UUID
)

final class ModuleTaughtWithTable(tag: Tag)
    extends Table[ModuleTaughtWithDbEntry](tag, "module_taught_with") {

  def module = column[UUID]("module", O.PrimaryKey)

  def moduleTaught = column[UUID]("module_taught", O.PrimaryKey)

  override def * = (
    module,
    moduleTaught
  ) <> (ModuleTaughtWithDbEntry.apply, ModuleTaughtWithDbEntry.unapply)
}
