package database.table

import basedata.Language
import slick.jdbc.PostgresProfile.api._

final class LanguageTable(tag: Tag) extends Table[Language](tag, "language") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (Language.tupled, Language.unapply)
}
