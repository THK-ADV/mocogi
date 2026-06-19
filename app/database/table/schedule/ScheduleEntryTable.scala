package database.table.schedule

import java.time.Instant
import java.util.UUID

import database.table.scheduleEntrySeriesIdColumnType
import database.Schema
import models.schedule.CourseType
import models.schedule.ScheduleEntrySeriesId
import play.api.libs.json.JsValue
import slick.jdbc.PostgresProfile.api.*

private[database] case class ScheduleEntryDbEntry(
    id: UUID,
    seriesId: ScheduleEntrySeriesId,
    module: UUID,
    courseType: CourseType,
    rooms: List[UUID],
    lecturer: List[String],
    start: Instant,
    end: Instant,
    po: JsValue,
    sourcePlanDraft: Option[UUID],
    sourceScheduleEntryDraft: Option[UUID]
)

private[database] final class ScheduleEntryTable(tag: Tag)
    extends Table[ScheduleEntryDbEntry](tag, Some(Schema.Schedule.name), "schedule_entry") {

  import database.table.given_BaseColumnType_CourseType
  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper

  def id = column[UUID]("id", O.PrimaryKey)

  def seriesId = column[ScheduleEntrySeriesId]("series_id")

  def start = column[Instant]("start", O.PrimaryKey)

  def end = column[Instant]("end")

  def module = column[UUID]("module")

  def courseType = column[CourseType]("course_type")

  def rooms = column[List[UUID]]("rooms")

  def lecturer = column[List[String]]("lecturer")

  def po = column[JsValue]("po")

  def sourcePlanDraft = column[Option[UUID]]("source_plan_draft")

  def sourceScheduleEntryDraft = column[Option[UUID]]("source_schedule_entry_draft")

  override def * = (
    id,
    seriesId,
    module,
    courseType,
    rooms,
    lecturer,
    start,
    end,
    po,
    sourcePlanDraft,
    sourceScheduleEntryDraft,
  ) <> (ScheduleEntryDbEntry.apply.tupled, ScheduleEntryDbEntry.unapply)
}
