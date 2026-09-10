package models.core

import models.AssessmentMethodSource
import play.api.libs.json.Format
import play.api.libs.json.Json

case class AssessmentMethodDefinition(
    id: String,
    deLabel: String,
    enLabel: String,
    source: AssessmentMethodSource
)

object AssessmentMethodDefinition {
  given Format[AssessmentMethodDefinition] = Json.format
}
