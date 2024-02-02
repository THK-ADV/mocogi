package models

import controllers.JsonNullWritable
import models.core.{Degree, IDLabel}
import play.api.libs.json.{Json, Writes}

case class StudyProgramView(
    poId: String,
    poVersion: Int,
    studyProgram: IDLabel,
    degree: Degree,
    specialization: Option[IDLabel]
) {
  def fullPoId: FullPoId = FullPoId(specialization.fold(poId)(_.id))
}

object StudyProgramView extends JsonNullWritable {
  implicit def writes: Writes[StudyProgramView] = Json.writes
}
