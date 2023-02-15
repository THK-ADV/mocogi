package controllers.formats

import models.core.Status
import play.api.libs.json.{Format, Json}

trait StatusFormat {
  implicit val statusFormat: Format[Status] =
    Json.format[Status]
}
