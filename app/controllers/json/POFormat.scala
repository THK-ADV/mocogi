package controllers.json

import basedata.PO
import play.api.libs.json.{Format, Json}

trait POFormat {
  implicit val poFormat: Format[PO] =
    Json.format[PO]
}
