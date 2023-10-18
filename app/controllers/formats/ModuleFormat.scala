package controllers.formats

import models.Module
import play.api.libs.json.{Format, Json}

trait ModuleFormat {
  implicit val moduleFormat: Format[Module] =
    Json.format[Module]
}
