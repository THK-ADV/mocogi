package models

import controllers.formats.GradesFormat
import models.core.{AbbrevLabelLike, Grade}
import play.api.libs.json.{Json, Writes}

case class StudyProgramShort(
    abbrev: String,
    deLabel: String,
    enLabel: String,
    grade: Grade
) extends AbbrevLabelLike

object StudyProgramShort extends GradesFormat {
  implicit def writes: Writes[StudyProgramShort] = Json.writes
}