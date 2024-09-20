package models.core

import java.time.LocalDate

import controllers.JsonNullWritable
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class PO(
    id: String,
    version: Int,
    program: String,
    dateFrom: LocalDate,
    dateTo: Option[LocalDate]
)

object PO extends JsonNullWritable {
  implicit def writes: Writes[PO] = Json.writes
}
