package database.table

import models.core.Language
import slick.jdbc.PostgresProfile.api._

final class LanguageTable(tag: Tag)
    extends Table[Language](tag, "language")
    with IDLabelColumn[Language] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((Language.apply _).tupled, Language.unapply)
}
