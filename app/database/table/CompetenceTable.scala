package database.table

import models.core.Competence
import slick.jdbc.PostgresProfile.api._

final class CompetenceTable(tag: Tag)
    extends Table[Competence](tag, "competence")
    with AbbrevLabelDescColumn[Competence] {
  override def * = (
    abbrev,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (Competence.tupled, Competence.unapply)
}
