package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

case class ModuleCompetenceDbEntry(
    module: UUID,
    competence: String
)

final class ModuleCompetenceTable(tag: Tag) extends Table[ModuleCompetenceDbEntry](tag, "module_competence") {

  def module = column[UUID]("module", O.PrimaryKey)

  def competence = column[String]("competence", O.PrimaryKey)

  override def * = (
    module,
    competence
  ) <> (ModuleCompetenceDbEntry.apply, ModuleCompetenceDbEntry.unapply)
}
