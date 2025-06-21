package models

import models.ModuleWorkload.totalHours
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

case class ModuleWorkload(
    lecture: Int,
    seminar: Int,
    practical: Int,
    exercise: Int,
    projectSupervision: Int,
    projectWork: Int
) {
  def sum() = lecture + seminar + practical + exercise + projectSupervision + projectWork

  def selfStudy(ects: Double, ectsFactor: Int): Int = selfStudy(totalHours(ects, ectsFactor))

  def selfStudy(totalHours: Int): Int = totalHours - sum()
}

object ModuleWorkload {
  implicit def writes: Writes[ModuleWorkload] = Json.writes

  implicit def reads: Reads[ModuleWorkload] = Json.reads

  implicit def format: Format[ModuleWorkload] = Format(reads, writes)

  def totalHours(ects: Double, ectsFactor: Int): Int = (ects * ectsFactor).toInt
}
