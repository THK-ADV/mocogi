package parsing.types

import play.api.libs.json.{Format, Json}

case class Participants(min: Int, max: Int)

object Participants {
  implicit def format: Format[Participants] = Json.format
}
