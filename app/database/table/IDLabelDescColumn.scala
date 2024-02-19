package database.table

import models.core.IDLabelDesc
import slick.jdbc.PostgresProfile.api._

trait IDLabelDescColumn[A <: IDLabelDesc] extends IDLabelColumn[A] {
  self: Table[_] =>

  def deDesc = column[String]("de_desc")

  def enDesc = column[String]("en_desc")
}
