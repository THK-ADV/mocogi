package database.table.schedule

import java.time.LocalDateTime
import java.util.UUID

import database.Schema
import models.schedule
import models.schedule.CourseType
import models.schedule.ScheduleEntry
import play.api.libs.json.JsValue
import slick.jdbc.PostgresProfile.api.*

private[database] final class ScheduleEntryTable(tag: Tag)
    extends Table[ScheduleEntry.DB](tag, Some(Schema.Schedule.name), "schedule_entry") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper

  given BaseColumnType[CourseType] =
    MappedColumnType.base[CourseType, String](_.id, CourseType.apply)

  def id = column[UUID]("id", O.PrimaryKey)

  def start = column[LocalDateTime]("start", O.PrimaryKey)

  def end = column[LocalDateTime]("end")

  def module = column[UUID]("module")

  def courseType = column[CourseType]("course_type")

  def rooms = column[List[UUID]]("rooms")

  def props = column[JsValue]("props")

  override def * = (
    id,
    module,
    courseType,
    rooms,
    start,
    end,
    props,
  ) <> (mapRow, unmapRow)

  private def mapRow: (
      (
          UUID,
          UUID,
          CourseType,
          List[UUID],
          LocalDateTime,
          LocalDateTime,
          JsValue,
      )
  ) => ScheduleEntry.DB = {
    case (
          id,
          module,
          courseType,
          rooms,
          start,
          end,
          props,
        ) =>
      ScheduleEntry(
        id,
        module,
        courseType,
        rooms,
        start,
        end,
        props,
      )
  }

  private def unmapRow(arg: ScheduleEntry.DB): Option[
    (
        UUID,
        UUID,
        CourseType,
        List[UUID],
        LocalDateTime,
        LocalDateTime,
        JsValue,
    )
  ] =
    Some(
      (
        arg.id,
        arg.module,
        arg.courseType,
        arg.rooms,
        arg.start,
        arg.end,
        arg.props,
      )
    )
}
