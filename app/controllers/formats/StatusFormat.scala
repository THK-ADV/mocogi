package controllers.formats

import basedata.Status
import play.api.libs.json.{Format, Json}

trait StatusFormat {
  implicit val statusFormat: Format[Status] =
    Json.format[Status]
}
