package database.table

import basedata.AbbrevLabelDescLike
import slick.jdbc.PostgresProfile.api._

trait AbbrevLabelDescColumn[A <: AbbrevLabelDescLike] {
  self: Table[_] =>

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  def deDesc = column[String]("de_desc")

  def enDesc = column[String]("en_desc")
}
