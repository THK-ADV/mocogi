package models

import controllers.JsonNullWritable
import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModulePOMandatoryProtocol(
    po: String,
    specialization: Option[String],
    recommendedSemester: List[Int]
) {
  def fullPo = specialization.fold(po)(identity)
}

object ModulePOMandatoryProtocol extends JsonNullWritable {
  implicit def format: Format[ModulePOMandatoryProtocol] = Json.format
}
