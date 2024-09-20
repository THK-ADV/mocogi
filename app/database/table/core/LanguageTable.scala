package database.table.core

import database.table.IDLabelColumn
import models.core.ModuleLanguage
import slick.jdbc.PostgresProfile.api._

final class LanguageTable(tag: Tag) extends Table[ModuleLanguage](tag, "language") with IDLabelColumn[ModuleLanguage] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((ModuleLanguage.apply _).tupled, ModuleLanguage.unapply)
}
