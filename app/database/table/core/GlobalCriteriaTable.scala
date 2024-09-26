package database.table.core

import database.table.IDLabelDescColumn
import models.core.ModuleGlobalCriteria
import slick.jdbc.PostgresProfile.api._

final class GlobalCriteriaTable(tag: Tag)
    extends Table[ModuleGlobalCriteria](tag, "global_criteria")
    with IDLabelDescColumn[ModuleGlobalCriteria] {
  override def * = (
    id,
    deLabel,
    deDesc,
    enLabel,
    enDesc
  ) <> (ModuleGlobalCriteria.apply.tupled, ModuleGlobalCriteria.unapply)
}
