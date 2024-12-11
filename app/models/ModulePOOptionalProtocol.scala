package models

import java.util.UUID

import controllers.JsonNullWritable
import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModulePOOptionalProtocol(
    po: String,
    specialization: Option[String],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
) {
  def fullPo = specialization.fold(po)(identity)
}

object ModulePOOptionalProtocol extends JsonNullWritable {
  implicit def format: Format[ModulePOOptionalProtocol] = Json.format
}
