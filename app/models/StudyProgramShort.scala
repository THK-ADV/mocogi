package models

import models.core.{Grade, IDLabel}
import play.api.libs.json.{Json, Writes}

case class StudyProgramShort(
    id: String,
    deLabel: String,
    enLabel: String,
    grade: Grade
) extends IDLabel

object StudyProgramShort {
  implicit def writes: Writes[StudyProgramShort] = Json.writes

  def apply(t: (String, String, String, Grade)): StudyProgramShort =
    StudyProgramShort(t._1, t._2, t._3, t._4)
}
