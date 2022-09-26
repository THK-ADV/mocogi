package controllers.json

import parsing.types.POMandatory
import play.api.libs.json.{Format, Json}

trait POMandatoryFormat {
  implicit val poMandatoryFormat: Format[POMandatory] =
    Json.format[POMandatory]
}
