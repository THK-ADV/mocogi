package models.core

import cats.data.NonEmptyList
import controllers.NelWrites
import play.api.libs.json.JsString
import play.api.libs.json.Writes

object ExamPhases:
  opaque type ExamPhase = String

  object ExamPhase extends NelWrites:
    lazy val none: ExamPhase = "none"

    def all: NonEmptyList[ExamPhase] =
      NonEmptyList.of(
        "wise_1",
        "sose_1",
        "sose_2",
        "off_wise",
        "off_sose",
        "off_schedule",
        none
      )

    given Ordering[ExamPhase] = Ordering.String

    given Writes[ExamPhase] = Writes(JsString.apply)

  extension (self: ExamPhase) def id: String = self
