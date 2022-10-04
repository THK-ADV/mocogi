package controllers.json

import basedata.Grade
import play.api.libs.json.{Format, Json}

trait GradesFormat {
  implicit val gradesFormat: Format[Grade] =
    Json.format[Grade]
}
