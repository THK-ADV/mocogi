package database.table

import java.time.LocalDate
import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] case class ExamListDbEntry(id: UUID, po: String, semester: String, date: LocalDate, url: String)

private[database] final class ExamListTable(tag: Tag)
    extends Table[ExamListDbEntry](tag, Some(Schema.Exams.name), "exam_list") {

  def id = column[UUID]("id", O.PrimaryKey)

  def po = column[String]("po")

  def semester = column[String]("semester")

  def date = column[LocalDate]("date")

  def url = column[String]("url")

  override def * = (id, po, semester, date, url) <> (ExamListDbEntry.apply, ExamListDbEntry.unapply)
}
