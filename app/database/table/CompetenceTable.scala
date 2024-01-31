package database.table

import models.core.Competence
import slick.jdbc.PostgresProfile.api._

final class CompetenceTable(tag: Tag)
    extends Table[Competence](tag, "competence")
    with IDLabelDescColumn[Competence] {
  override def * = (
    id,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> ((Competence.apply _).tupled, Competence.unapply)
}
