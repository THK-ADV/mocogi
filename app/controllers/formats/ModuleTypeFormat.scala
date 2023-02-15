package controllers.formats

import models.core.ModuleType
import play.api.libs.json.{Format, Json}

trait ModuleTypeFormat {
  implicit val moduleTypeFormat: Format[ModuleType] =
    Json.format[ModuleType]
}
