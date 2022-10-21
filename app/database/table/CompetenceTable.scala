package database.table

import basedata.Competence
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
