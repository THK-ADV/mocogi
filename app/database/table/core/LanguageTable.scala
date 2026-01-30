package database.table.core

import database.table.IDLabelColumn
import database.Schema
import models.core.ModuleLanguage
import slick.jdbc.PostgresProfile.api.*

private[database] final class LanguageTable(tag: Tag)
    extends Table[ModuleLanguage](tag, Some(Schema.Core.name), "language")
    with IDLabelColumn[ModuleLanguage] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (ModuleLanguage.apply.tupled, ModuleLanguage.unapply)
}
