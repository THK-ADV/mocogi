package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ModuleCompetenceDbEntry(
    module: UUID,
    competence: String
)

final class ModuleCompetenceTable(tag: Tag)
    extends Table[ModuleCompetenceDbEntry](tag, "module_competence") {

  def module = column[UUID]("module", O.PrimaryKey)

  def competence = column[String]("competence", O.PrimaryKey)

  override def * = (
    module,
    competence
  ) <> (ModuleCompetenceDbEntry.tupled, ModuleCompetenceDbEntry.unapply)
}
