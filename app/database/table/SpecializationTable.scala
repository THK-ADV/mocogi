package database.table

import models.core.Specialization
import slick.jdbc.PostgresProfile.api._

final class SpecializationTable(tag: Tag)
    extends Table[Specialization](tag, "specialization") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def label = column[String]("label")

  def po = column[String]("po")

  override def * = (
    abbrev,
    label,
    po
  ) <> ((Specialization.apply _).tupled, Specialization.unapply)
}
