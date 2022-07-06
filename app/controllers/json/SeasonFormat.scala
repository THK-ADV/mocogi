package controllers.json

import parsing.types.Season
import play.api.libs.json.{Format, Json}

trait SeasonFormat {
  implicit val seasonFormat: Format[Season] =
    Json.format[Season]
}
