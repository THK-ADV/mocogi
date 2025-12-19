package models

import controllers.json.JsonNullWritable
import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleAssessmentMethodEntryProtocol(
    method: String,
    percentage: Option[Double],
    precondition: List[String]
)

object ModuleAssessmentMethodEntryProtocol extends JsonNullWritable {
  implicit val format: Format[ModuleAssessmentMethodEntryProtocol] = Json.format
}
