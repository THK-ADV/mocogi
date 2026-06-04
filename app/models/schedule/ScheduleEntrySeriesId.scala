package models.schedule

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.Json

/**
 * Groups related occurrences of the same schedule entry (e.g. weekly duplicates).
 * Shared UUID only; not a recurrence rule. Used so the calendar can offer
 * "edit this slot" vs "edit all slots in this series" on live entries.
 */
opaque type ScheduleEntrySeriesId = UUID

object ScheduleEntrySeriesId {
  def apply(value: UUID): ScheduleEntrySeriesId = value

  def random(): ScheduleEntrySeriesId = UUID.randomUUID()

  extension (id: ScheduleEntrySeriesId) {
    def value: UUID  = id
    def toUUID: UUID = id.value
  }

  given Format[ScheduleEntrySeriesId] = new Format[ScheduleEntrySeriesId] {
    def reads(json: JsValue): JsResult[ScheduleEntrySeriesId] = json.validate[UUID].map(apply)
    def writes(id: ScheduleEntrySeriesId): JsValue            = Json.toJson(id.toUUID)
  }
}
