package database.table

import java.time.LocalDate

import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] case class ExamListDbEntry(po: String, semester: String, date: LocalDate, url: String)

private[database] final class ExamListTable(tag: Tag)
    extends Table[ExamListDbEntry](tag, Some(Schema.Modules.name), "exam_list") {

  def po = column[String]("po", O.PrimaryKey)

  def semester = column[String]("semester", O.PrimaryKey)

  def date = column[LocalDate]("date")

  def url = column[String]("url")

  override def * = (po, semester, date, url) <> (ExamListDbEntry.apply, ExamListDbEntry.unapply)
}
