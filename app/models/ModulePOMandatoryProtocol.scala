package models

import controllers.JsonNullWritable
import play.api.libs.json.{Format, Json}

case class ModulePOMandatoryProtocol(
    po: String,
    specialization: Option[String],
    recommendedSemester: List[Int]
)

object ModulePOMandatoryProtocol extends JsonNullWritable {
  implicit def format: Format[ModulePOMandatoryProtocol] = Json.format
}
