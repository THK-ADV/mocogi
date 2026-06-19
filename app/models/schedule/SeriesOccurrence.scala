package models.schedule

import java.time.Instant
import java.util.UUID

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class SeriesOccurrence(id: UUID, start: Instant, end: Instant)

object SeriesOccurrence {
  given Writes[SeriesOccurrence] = Json.writes
}
