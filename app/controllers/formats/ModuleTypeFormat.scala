package controllers.formats

import basedata.ModuleType
import play.api.libs.json.{Format, Json}

trait ModuleTypeFormat {
  implicit val moduleTypeFormat: Format[ModuleType] =
    Json.format[ModuleType]
}
