package models

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleAssessmentMethodsProtocol(
    mandatory: List[ModuleAssessmentMethodEntryProtocol],
    @Deprecated(forRemoval = true) optional: List[ModuleAssessmentMethodEntryProtocol]
)

object ModuleAssessmentMethodsProtocol {
  implicit val format: Format[ModuleAssessmentMethodsProtocol] = Json.format
}
