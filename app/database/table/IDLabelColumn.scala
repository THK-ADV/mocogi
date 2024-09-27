package database.table

import slick.jdbc.PostgresProfile.api._

trait IDLabelColumn[A] {
  self: Table[?] =>

  def id = column[String]("id", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")
}
