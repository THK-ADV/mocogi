package models.schedule

import java.time.Instant
import java.util.UUID

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

case class ScheduleEntryDraft[ID](
    id: ID,
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

object ScheduleEntryDraft {
  type JSON = ScheduleEntryDraft[Option[UUID]]
  type DB   = ScheduleEntryDraft[UUID]

  given Reads[JSON] = Json.reads
  given Writes[DB]  = Json.writes
}
