package database.table

import models.core.PO
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDate

final class POTable(tag: Tag) extends Table[PO](tag, "po") {

  def id = column[String]("id", O.PrimaryKey)

  def studyProgram = column[String]("study_program")

  def version = column[Int]("version")

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
    version,
    studyProgram,
    dateFrom,
    dateTo
  ) <> ((PO.apply _).tupled, PO.unapply)
}
