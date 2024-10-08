package models.core

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class AssessmentMethod(id: String, deLabel: String, enLabel: String) extends IDLabel

object AssessmentMethod {
  implicit def writes: Writes[AssessmentMethod] =
    Json.writes
}
