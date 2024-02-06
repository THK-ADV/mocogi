package database.table.core

import database.table.IDLabelDescColumn
import models.core.GlobalCriteria
import slick.jdbc.PostgresProfile.api._

final class GlobalCriteriaTable(tag: Tag)
    extends Table[GlobalCriteria](tag, "global_criteria")
    with IDLabelDescColumn[GlobalCriteria] {
  override def * = (
    id,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> ((GlobalCriteria.apply _).tupled, GlobalCriteria.unapply)
}
