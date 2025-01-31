package models

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json

case class PermittedAssessmentMethodForModule(module: UUID, assessmentMethods: List[String])

object PermittedAssessmentMethodForModule {
  given Format[PermittedAssessmentMethodForModule] = Json.format
}
