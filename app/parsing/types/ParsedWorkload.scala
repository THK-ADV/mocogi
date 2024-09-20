package parsing.types

import play.api.libs.json.Format
import play.api.libs.json.Json

case class ParsedWorkload(
    lecture: Int,
    seminar: Int,
    practical: Int,
    exercise: Int,
    projectSupervision: Int,
    projectWork: Int
)

object ParsedWorkload {
  implicit def format: Format[ParsedWorkload] = Json.format
}
