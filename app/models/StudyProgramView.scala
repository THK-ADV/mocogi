package models

import controllers.JsonNullWritable
import models.core.Degree
import models.core.IDLabel
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class StudyProgramView(
    id: String,
    deLabel: String,
    enLabel: String,
    abbreviation: String,
    po: POCore,
    degree: Degree,
    specialization: Option[IDLabel]
) extends IDLabel {
  def fullPoId: FullPoId = FullPoId(specialization.fold(po.id)(_.id))
}

object StudyProgramView extends JsonNullWritable {
  implicit def writes: Writes[StudyProgramView] = Json.writes
}
