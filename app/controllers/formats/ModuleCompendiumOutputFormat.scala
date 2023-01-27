package controllers.formats

import database.ModuleCompendiumOutput
import play.api.libs.json.{Format, Json}

trait ModuleCompendiumOutputFormat
    extends MetadataOutputFormat
    with ContentFormat {
  implicit val metaDataOutputFormat: Format[ModuleCompendiumOutput] =
    Json.format[ModuleCompendiumOutput]
}
