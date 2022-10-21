package database.table

import database.entities.ECTSDbEntry
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ECTSTable(tag: Tag) extends Table[ECTSDbEntry](tag, "ects") {

  def id = column[UUID]("id", O.PrimaryKey)

  def value = column[Double]("value")

  override def * = (id, value) <> (ECTSDbEntry.tupled, ECTSDbEntry.unapply)
}
