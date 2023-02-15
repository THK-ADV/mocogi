package controllers.formats

import models.core.Grade
import play.api.libs.json.{Format, Json}

trait GradesFormat {
  implicit val gradesFormat: Format[Grade] =
    Json.format[Grade]
}
