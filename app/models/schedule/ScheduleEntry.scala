package models.schedule

import java.time.Instant
import java.util.UUID

import controllers.json.JsonNullWritable
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads

case class ScheduleEntry[ID](
    id: ID,
    seriesId: ScheduleEntrySeriesId,
    module: UUID,
    courseType: CourseType,
    rooms: List[UUID],
    start: Instant,
    end: Instant,
    props: JsValue,
    sourcePlanDraft: Option[UUID],
    sourceScheduleEntryDraft: Option[UUID]
)

object ScheduleEntry extends JsonNullWritable {
  type JSON = ScheduleEntry[Option[UUID]]
  type DB   = ScheduleEntry[UUID]

  given Reads[JSON] = Json.reads
}
