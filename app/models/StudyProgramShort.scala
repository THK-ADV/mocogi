package models

import models.core.{Degree, IDLabel}
import play.api.libs.json.{Json, Writes}

// TODO remove
case class StudyProgramShort(
    id: String,
    deLabel: String,
    enLabel: String,
    degree: Degree
) extends IDLabel

object StudyProgramShort {
  implicit def writes: Writes[StudyProgramShort] = Json.writes

  def apply(t: (String, String, String, Degree)): StudyProgramShort =
    StudyProgramShort(t._1, t._2, t._3, t._4)
}
