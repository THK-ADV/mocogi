package models

import controllers.JsonNullWritable
import play.api.libs.json.{Format, Json}

case class ModuleAssessmentMethodEntryProtocol(
    method: String,
    percentage: Option[Double],
    precondition: List[String]
)

object ModuleAssessmentMethodEntryProtocol extends JsonNullWritable {
  implicit val format: Format[ModuleAssessmentMethodEntryProtocol] = Json.format
}
