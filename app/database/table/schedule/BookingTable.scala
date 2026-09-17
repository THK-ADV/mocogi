package database.table.schedule

import java.time.Instant
import java.util.UUID

import database.table.scheduleEntrySeriesIdColumnType
import database.Schema
import models.schedule.BookingKind
import models.schedule.CourseType
import models.schedule.ScheduleEntrySeriesId
import play.api.libs.json.JsValue
import slick.jdbc.PostgresProfile.api.*

private[database] case class BookingDbEntry(
    id: UUID,
    kind: BookingKind,
    seriesId: ScheduleEntrySeriesId,
    title: String,
    note: Option[String],
    rooms: List[UUID],
    lecturer: List[String],
    start: Instant,
    end: Instant,
    module: Option[UUID],
    courseType: Option[CourseType],
    po: Option[JsValue],
    createdBy: String,
    updatedAt: Instant
)

private[database] final class BookingTable(tag: Tag)
    extends Table[BookingDbEntry](tag, Some(Schema.Schedule.name), "booking") {
  import database.table.given_BaseColumnType_BookingKind
  import database.table.given_BaseColumnType_CourseType
  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper

  def id         = column[UUID]("id", O.PrimaryKey)
  def kind       = column[BookingKind]("kind")
  def seriesId   = column[ScheduleEntrySeriesId]("series_id")
  def title      = column[String]("title")
  def note       = column[Option[String]]("note")
  def rooms      = column[List[UUID]]("rooms")
  def lecturer   = column[List[String]]("lecturer")
  def start      = column[Instant]("start")
  def end        = column[Instant]("end")
  def module     = column[Option[UUID]]("module")
  def courseType = column[Option[CourseType]]("course_type")
  def po         = column[Option[JsValue]]("po")
  def createdBy  = column[String]("created_by")
  def updatedAt  = column[Instant]("updated_at")

  override def * =
    (id, kind, seriesId, title, note, rooms, lecturer, start, end, module, courseType, po, createdBy, updatedAt) <>
      (BookingDbEntry.apply.tupled, BookingDbEntry.unapply)
}
