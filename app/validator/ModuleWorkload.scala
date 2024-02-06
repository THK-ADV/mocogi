package validator

import play.api.libs.json.{Format, Json}

case class ModuleWorkload(
    lecture: Int,
    seminar: Int,
    practical: Int,
    exercise: Int,
    projectSupervision: Int,
    projectWork: Int,
    selfStudy: Int,
    total: Int
)

object ModuleWorkload {
  implicit def format: Format[ModuleWorkload] = Json.format
}
