package models

import controllers.JsonNullWritable
import play.api.libs.json.Format
import play.api.libs.json.Json

// TODO use ModuleAssessmentMethodEntry with type arguments
case class ModuleAssessmentMethodEntryProtocol(
    method: String,
    percentage: Option[Double],
    precondition: List[String]
)

object ModuleAssessmentMethodEntryProtocol extends JsonNullWritable {
  implicit val format: Format[ModuleAssessmentMethodEntryProtocol] = Json.format
}
