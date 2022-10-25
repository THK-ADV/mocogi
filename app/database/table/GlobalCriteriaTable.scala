package database.table

import basedata.GlobalCriteria
import slick.jdbc.PostgresProfile.api._

final class GlobalCriteriaTable(tag: Tag)
    extends Table[GlobalCriteria](tag, "global_criteria")
    with AbbrevLabelDescColumn[GlobalCriteria] {
  override def * = (
    abbrev,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (GlobalCriteria.tupled, GlobalCriteria.unapply)
}
