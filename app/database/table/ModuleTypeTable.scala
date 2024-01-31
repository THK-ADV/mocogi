package database.table

import models.core.ModuleType
import slick.jdbc.PostgresProfile.api._

final class ModuleTypeTable(tag: Tag)
    extends Table[ModuleType](tag, "module_type")
    with IDLabelColumn[ModuleType] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((ModuleType.apply _).tupled, ModuleType.unapply)
}
