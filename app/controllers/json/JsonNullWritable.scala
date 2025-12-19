package controllers.json

import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.OptionHandlers

trait JsonNullWritable {
  implicit val config: Aux[Json.MacroOptions] =
    JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)
}
