package controllers.formats

import models.core.Location
import play.api.libs.json.{Format, Json}

trait LocationFormat {
  implicit val locationFormat: Format[Location] =
    Json.format[Location]
}
