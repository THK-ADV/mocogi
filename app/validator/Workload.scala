package validator

import play.api.libs.json.{Json, Writes}

case class Workload(
    lecture: Int,
    seminar: Int,
    practical: Int,
    exercise: Int,
    projectSupervision: Int,
    projectWork: Int,
    selfStudy: Int,
    total: Int
)

object Workload {
  implicit def writes: Writes[Workload] = Json.writes
}
