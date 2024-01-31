package parsing.types

import models.core.AssessmentMethod
import play.api.libs.json.{Json, Writes}

case class AssessmentMethods(
    mandatory: List[AssessmentMethodEntry],
    optional: List[AssessmentMethodEntry]
)

object AssessmentMethods {
  implicit def writes: Writes[AssessmentMethods] = Json.writes
}

case class AssessmentMethodEntry(
    method: AssessmentMethod,
    percentage: Option[Double],
    precondition: List[AssessmentMethod]
)

object AssessmentMethodEntry {
  implicit def writes: Writes[AssessmentMethodEntry] = Json.writes
}
