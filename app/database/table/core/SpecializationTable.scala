package database.table.core

import database.Schema
import models.core.Specialization
import slick.jdbc.PostgresProfile.api.*

private[database] final class SpecializationTable(tag: Tag)
    extends Table[Specialization](tag, Some(Schema.Core.name), "specialization") {

  def id = column[String]("id", O.PrimaryKey)

  def label = column[String]("label")

  def abbreviation = column[String]("abbreviation")

  def po = column[String]("po")

  override def * = (
    id,
    label,
    abbreviation,
    po
  ) <> (Specialization.apply.tupled, Specialization.unapply)
}
