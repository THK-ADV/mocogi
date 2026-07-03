package models.schedule

import java.time.Instant
import java.util.UUID

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Reads

/**
 * Shared JSON write model for live schedule entries and schedule entry drafts.
 *
 * Used as the request payload for POST and PUT operations on both
 * `/scheduleEntries` and `/scheduleEntriesDrafts`. Draft-specific context,
 * such as the plan draft ID, is resolved and validated separately.
 */
case class ScheduleEntryProtocol(
    seriesId: ScheduleEntrySeriesId,
    module: UUID,
    courseType: CourseType,
    rooms: List[UUID],
    lecturer: List[String],
    start: Instant,
    end: Instant,
    po: JsValue,
)

object ScheduleEntryProtocol {
  given Reads[ScheduleEntryProtocol] = Json.reads
}
