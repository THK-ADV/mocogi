package models

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

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
  implicit def writes: Writes[ModuleWorkload] = Json.writes

  implicit def reads: Reads[ModuleWorkload] = js =>
    for {
      lecture            <- js.\("lecture").validate[Int]
      seminar            <- js.\("seminar").validate[Int]
      practical          <- js.\("practical").validate[Int]
      exercise           <- js.\("exercise").validate[Int]
      projectSupervision <- js.\("projectSupervision").validate[Int]
      projectWork        <- js.\("projectWork").validate[Int]
    } yield ModuleWorkload(
      lecture,
      seminar,
      practical,
      exercise,
      projectSupervision,
      projectWork,
      Int.MinValue,
      Int.MinValue
    )

  implicit def format: Format[ModuleWorkload] = Format(reads, writes)
}
