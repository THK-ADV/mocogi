package models.core

import java.time.LocalDate

import controllers.json.JsonNullWritable
import play.api.libs.json.Format
import play.api.libs.json.Json

case class PO(
    id: String,
    version: Int,
    program: String,
    dateFrom: LocalDate,
    dateTo: Option[LocalDate],
    ectsFactor: Int
)

object PO extends JsonNullWritable {
  given Format[PO] = Json.format
}
