package database.table.schedule

import java.time.Instant
import java.util.UUID

import database.table.given_BaseColumnType_CourseType
import database.table.scheduleEntrySeriesIdColumnType
import database.Schema
import models.schedule.CourseType
import models.schedule.ScheduleEntrySeriesId
import play.api.libs.json.JsValue
import slick.jdbc.PostgresProfile.api.*

private[database] case class ScheduleEntryDraftDbEntry(
    id: UUID,
    planDraft: UUID,
    seriesId: ScheduleEntrySeriesId,
    module: UUID,
    courseType: CourseType,
    rooms: List[UUID],
    lecturer: List[String],
    start: Instant,
    end: Instant,
    po: JsValue
)

private[database] final class ScheduleEntryDraftTable(tag: Tag)
    extends Table[ScheduleEntryDraftDbEntry](tag, Some(Schema.Schedule.name), "schedule_entry_draft") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper

  def id = column[UUID]("id", O.PrimaryKey)

  def planDraft = column[UUID]("plan_draft")

  def seriesId = column[ScheduleEntrySeriesId]("series_id")

  def module = column[UUID]("module")

  def courseType = column[CourseType]("course_type")

  def rooms = column[List[UUID]]("rooms")

  def lecturer = column[List[String]]("lecturer")

  def start = column[Instant]("start")

  def end = column[Instant]("end")

  def po = column[JsValue]("po")

  override def * = (
    id,
    planDraft,
    seriesId,
    module,
    courseType,
    rooms,
    lecturer,
    start,
    end,
    po,
  ) <> (ScheduleEntryDraftDbEntry.apply.tupled, ScheduleEntryDraftDbEntry.unapply)
}
