package controllers.json

import parsing.types.Location
import play.api.libs.json.{Format, Json}

trait LocationFormat {
  implicit val locationFormat: Format[Location] =
    Json.format[Location]
}
