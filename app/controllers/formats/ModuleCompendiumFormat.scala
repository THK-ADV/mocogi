package controllers.formats

import parsing.types._
import play.api.libs.json.{Format, Json}

trait ModuleCompendiumFormat extends MetadataFormat with ContentFormat {
  implicit val moduleCompendiumFormat: Format[ModuleCompendium] =
    Json.format[ModuleCompendium]
}
