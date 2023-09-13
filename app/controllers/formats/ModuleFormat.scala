package controllers.formats

import play.api.libs.json.{Format, Json}

trait ModuleFormat {
  implicit val moduleFormat: Format[validator.Module] =
    Json.format[validator.Module]

  implicit val module2Format: Format[service.Module] =
    Json.format[service.Module]
}
