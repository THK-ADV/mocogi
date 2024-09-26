package database.table.core

import database.table.IDLabelDescColumn
import models.core.ModuleCompetence
import slick.jdbc.PostgresProfile.api._

final class CompetenceTable(tag: Tag)
    extends Table[ModuleCompetence](tag, "competence")
    with IDLabelDescColumn[ModuleCompetence] {
  override def * = (
    id,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (ModuleCompetence.apply.tupled, ModuleCompetence.unapply)
}
