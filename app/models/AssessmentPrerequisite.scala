package models

import play.api.libs.json.Format
import play.api.libs.json.Json

case class AssessmentPrerequisite(modules: String, reason: String)

object AssessmentPrerequisite {
  given Format[AssessmentPrerequisite] = Json.format
}
