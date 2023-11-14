package database.table

import slick.jdbc.PostgresProfile.api._

import java.time.LocalDate

case class POModificationDateDbEntry(po: String, date: LocalDate)

final class POModificationDateTable(tag: Tag)
    extends Table[POModificationDateDbEntry](tag, "po_modification_date") {

  def po = column[String]("po", O.PrimaryKey)

  def date = column[LocalDate]("date", O.PrimaryKey)

  override def * = (
    po,
    date
  ) <> (POModificationDateDbEntry.tupled, POModificationDateDbEntry.unapply)
}
