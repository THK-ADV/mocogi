package database.table

import slick.jdbc.PostgresProfile.api._

trait AbbrevLabelColumn[A] {
  self: Table[_] =>

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")
}
