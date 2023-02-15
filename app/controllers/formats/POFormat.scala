package controllers.formats

import models.core.PO
import play.api.libs.json.{Format, Json}

trait POFormat extends JsonNullWritable {
  implicit val poFormat: Format[PO] =
    Json.format[PO]
}
