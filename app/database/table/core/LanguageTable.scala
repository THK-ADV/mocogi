package database.table.core

import database.table.IDLabelColumn
import models.core.ModuleLanguage
import slick.jdbc.PostgresProfile.api._

private[database] final class LanguageTable(tag: Tag)
    extends Table[ModuleLanguage](tag, "language")
    with IDLabelColumn[ModuleLanguage] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (ModuleLanguage.apply.tupled, ModuleLanguage.unapply)
}
