package database.table.core

import java.util.UUID

import database.Schema
import models.core.TeachingUnit
import slick.jdbc.PostgresProfile.api.*

private[database] final class TeachingUnitTable(tag: Tag)
    extends Table[TeachingUnit](tag, Some(Schema.Core.name), "teaching_unit") {

  def id = column[UUID]("id", O.PrimaryKey)

  def label = column[String]("label")

  def abbrev = column[String]("abbrev")

  def faculty = column[String]("faculty")

  override def * = (id, label, abbrev, faculty) <> (TeachingUnit.apply.tupled, TeachingUnit.unapply)
}
