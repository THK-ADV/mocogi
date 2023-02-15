package controllers.formats

import models.core.Season
import play.api.libs.json.{Format, Json}

trait SeasonFormat {
  implicit val seasonFormat: Format[Season] =
    Json.format[Season]
}
