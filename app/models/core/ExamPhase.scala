package models.core

import cats.data.NonEmptyList
import controllers.NelWrites
import play.api.libs.json.{Json, Writes}

case class ExamPhase(id: String) extends AnyVal

object ExamPhase extends NelWrites {
  lazy val none = ExamPhase("none")

  def all =
    NonEmptyList.of(
      ExamPhase("wise_1"),
      ExamPhase("wise_2"),
      ExamPhase("sose_1"),
      ExamPhase("sose_2"),
      none
    )

  implicit def writes: Writes[ExamPhase] = Json.writes
}
