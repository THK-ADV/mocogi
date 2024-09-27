package database.table.core

import models.core.Specialization
import slick.jdbc.PostgresProfile.api._

final class SpecializationTable(tag: Tag) extends Table[Specialization](tag, "specialization") {

  def id = column[String]("id", O.PrimaryKey)

  def label = column[String]("label")

  def po = column[String]("po")

  override def * = (
    id,
    label,
    po
  ) <> (Specialization.apply.tupled, Specialization.unapply)
}
