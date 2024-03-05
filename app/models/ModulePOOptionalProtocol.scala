package models

import controllers.JsonNullWritable
import play.api.libs.json.{Format, Json}

import java.util.UUID

case class ModulePOOptionalProtocol(
    po: String,
    specialization: Option[String],
    instanceOf: Option[UUID],
    isFocus: Boolean,
    recommendedSemester: List[Int]
)

object ModulePOOptionalProtocol extends JsonNullWritable {
  implicit def format: Format[ModulePOOptionalProtocol] = Json.format
}
