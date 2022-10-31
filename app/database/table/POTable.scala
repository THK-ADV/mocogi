package database.table

import slick.jdbc.PostgresProfile.api._

import java.time.LocalDate

case class PODbEntry(
    abbrev: String,
    studyProgram: String,
    version: Int,
    date: LocalDate,
    dateFrom: LocalDate,
    dateTo: Option[LocalDate]
)

final class POTable(tag: Tag) extends Table[PODbEntry](tag, "po") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def studyProgram = column[String]("study_program")

  def version = column[Int]("version")

  def date = column[LocalDate]("date")

  def dateFrom = column[LocalDate]("date_from")

  def dateTo = column[Option[LocalDate]]("date_to")

  override def * = (
    abbrev,
    studyProgram,
    version,
    date,
    dateFrom,
    dateTo
  ) <> (PODbEntry.tupled, PODbEntry.unapply)
}
