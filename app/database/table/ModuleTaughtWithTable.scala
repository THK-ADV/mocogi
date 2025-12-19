package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

private[database] case class ModuleTaughtWithDbEntry(
    module: UUID,
    moduleTaught: UUID
)

private[database] final class ModuleTaughtWithTable(tag: Tag)
    extends Table[ModuleTaughtWithDbEntry](tag, "module_taught_with") {

  def module = column[UUID]("module", O.PrimaryKey)

  def moduleTaught = column[UUID]("module_taught", O.PrimaryKey)

  override def * = (
    module,
    moduleTaught
  ) <> (ModuleTaughtWithDbEntry.apply, ModuleTaughtWithDbEntry.unapply)
}
