package controllers.formats

import play.api.libs.json.{Format, Json}
import validator.Module

trait ModuleFormat {
  implicit val moduleFormat: Format[Module] =
    Json.format[Module]
}
