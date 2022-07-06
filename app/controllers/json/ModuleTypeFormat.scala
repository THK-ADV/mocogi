package controllers.json

import parsing.types.ModuleType
import play.api.libs.json.{Format, Json}

trait ModuleTypeFormat {
  implicit val moduleTypeFormat: Format[ModuleType] =
    Json.format[ModuleType]
}
