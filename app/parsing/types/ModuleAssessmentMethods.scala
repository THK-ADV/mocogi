package parsing.types

import models.core.AssessmentMethod
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleAssessmentMethods(mandatory: List[ModuleAssessmentMethodEntry])

object ModuleAssessmentMethods {
  implicit def writes: Writes[ModuleAssessmentMethods] = Json.writes
}

case class ModuleAssessmentMethodEntry(
    method: AssessmentMethod,
    percentage: Option[Double],
    precondition: List[AssessmentMethod]
)

object ModuleAssessmentMethodEntry {
  implicit def writes: Writes[ModuleAssessmentMethodEntry] = Json.writes
}
