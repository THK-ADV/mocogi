package controllers.formats

import parsing.types.Content
import play.api.libs.json.{Format, Json}

trait ContentFormat {
  implicit val contentFormat: Format[Content] =
    Json.format[Content]
}
