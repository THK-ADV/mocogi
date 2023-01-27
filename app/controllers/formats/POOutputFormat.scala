package controllers.formats

import database.{POMandatoryOutput, POOptionalOutput, POOutput}
import play.api.libs.json.{Format, Json}

trait POOutputFormat {
  implicit val poMandatoryOutputFormat: Format[POMandatoryOutput] =
    Json.format[POMandatoryOutput]

  implicit val poOptionalOutputFormat: Format[POOptionalOutput] =
    Json.format[POOptionalOutput]

  implicit val poOutputFormat: Format[POOutput] =
    Json.format[POOutput]
}
