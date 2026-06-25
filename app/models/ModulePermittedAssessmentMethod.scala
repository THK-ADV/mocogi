package models

import java.util.UUID

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModulePermittedAssessmentMethod(module: UUID, assessmentMethods: List[String])

object ModulePermittedAssessmentMethod {
  given Format[ModulePermittedAssessmentMethod] = Json.format
}
