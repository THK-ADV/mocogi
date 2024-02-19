package models.core

import controllers.JsonNullWritable
import play.api.libs.json.{Json, Writes}

import java.time.LocalDate

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
