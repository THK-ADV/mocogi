package database.table

import basedata.Language
import slick.jdbc.PostgresProfile.api._

final class LanguageTable(tag: Tag)
    extends Table[Language](tag, "language")
    with AbbrevLabelColumn[Language] {
  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Language.tupled, Language.unapply)
}
