package models.core

import cats.data.NonEmptyList
import controllers.NelWrites
import play.api.libs.json.JsString
import play.api.libs.json.Writes

object ExamPhases:
  opaque type ExamPhase = String

  object ExamPhase extends NelWrites:
    def none: ExamPhase    = "none"
    def sose1: ExamPhase   = "sose_1"
    def sose2: ExamPhase   = "sose_2"
    def offWise: ExamPhase = "off_wise"
    def offSose: ExamPhase = "off_sose"

    def all: NonEmptyList[ExamPhase] =
      NonEmptyList.of(
        "wise_1",
        sose1,
        sose2,
        offWise,
        offSose,
        "off_schedule",
        none
      )

    given Ordering[ExamPhase] = Ordering.String

    given Writes[ExamPhase] = Writes(JsString.apply)

  extension (self: ExamPhase) def id: String = self
