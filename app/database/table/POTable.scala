package database.table

import slick.jdbc.PostgresProfile.api._

import java.time.LocalDate

case class PODbEntry(
    id: String,
    studyProgram: String,
    version: Int,
    date: LocalDate,
    dateFrom: LocalDate,
    dateTo: Option[LocalDate]
)

final class POTable(tag: Tag) extends Table[PODbEntry](tag, "po") {

  def id = column[String]("id", O.PrimaryKey)

  def studyProgram = column[String]("study_program")

  def version = column[Int]("version")

  def date = column[LocalDate]("date")

  def dateFrom = column[LocalDate]("date_from")

  def dateTo = column[Option[LocalDate]]("date_to")

  def isValid(date: LocalDate = LocalDate.now): Rep[Boolean] =
    this.dateFrom <= date && this.dateTo.map(_ >= date).getOrElse(true)

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.id
    )

  override def * = (
    id,
    studyProgram,
    version,
    date,
    dateFrom,
    dateTo
  ) <> (PODbEntry.tupled, PODbEntry.unapply)
}
