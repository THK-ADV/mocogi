package controllers.json

import parsing.types.Prerequisites
import play.api.libs.json.{Format, Json}

trait PrerequisitesFormat {
  implicit val prerequisitesFormat: Format[Prerequisites] =
    Json.format[Prerequisites]
}
