package controllers.formats

import models.ModuleCompendiumProtocol
import play.api.libs.json.{Format, Json}

trait ModuleCompendiumProtocolFormat
    extends MetadataProtocolFormat
    with ContentFormat {
  implicit val moduleCompendiumProtocolFormat
      : Format[ModuleCompendiumProtocol] =
    Json.format[ModuleCompendiumProtocol]
}
