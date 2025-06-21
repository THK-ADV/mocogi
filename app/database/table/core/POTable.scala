package database.table.core

import java.time.LocalDate

import models.core.PO
import slick.jdbc.PostgresProfile.api.*

final class POTable(tag: Tag) extends Table[PO](tag, "po") {

  def id = column[String]("id", O.PrimaryKey)

  def studyProgram = column[String]("study_program")

  def version = column[Int]("version")

  def dateFrom = column[LocalDate]("date_from")

  def dateTo = column[Option[LocalDate]]("date_to")

  def ectsFactor = column[Int]("ects_factor")

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
    dateTo,
    ectsFactor
  ) <> (PO.apply.tupled, PO.unapply)
}
