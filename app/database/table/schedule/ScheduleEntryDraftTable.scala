package database.table.schedule

import java.time.LocalDateTime
import java.util.UUID

import database.table.scheduleEntrySeriesIdColumnType
import database.Schema
import models.schedule.CourseType
import models.schedule.ScheduleEntryDraft
import models.schedule.ScheduleEntrySeriesId
import play.api.libs.json.JsValue
import slick.jdbc.PostgresProfile.api.*

private[database] final class ScheduleEntryDraftTable(tag: Tag)
    extends Table[ScheduleEntryDraft.DB](tag, Some(Schema.Schedule.name), "schedule_entry_draft") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper

  given BaseColumnType[CourseType] =
    MappedColumnType.base[CourseType, String](_.id, CourseType.apply)

  def id = column[UUID]("id", O.PrimaryKey)

  def planDraft = column[UUID]("plan_draft")

  def seriesId = column[ScheduleEntrySeriesId]("series_id")

  def module = column[UUID]("module")

  def courseType = column[CourseType]("course_type")

  def rooms = column[List[UUID]]("rooms")

  def lecturer = column[List[String]]("lecturer")

  def start = column[LocalDateTime]("start")

  def end = column[LocalDateTime]("end")

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
  ) <> (ScheduleEntryDraft.apply[UUID].tupled, ScheduleEntryDraft.unapply[UUID])
}
