package parsing.metadata

import cats.data.NonEmptyList
import models.core.ExamPhases.ExamPhase
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{EitherValues, OptionValues}

class ExamPhaseParserSpec
    extends AnyWordSpec
    with EitherValues
    with OptionValues {

  import ExamPhaseParser._

  implicit val all: List[ExamPhase] = List(ExamPhase.none)

  "A Exam Phase Parser" should {
    "parse a single examination phase" in {
      val input = "exam_phases: exam_phase.none"
      val (res, rest) = parser.parse(input)
      assert(res.value.size == 1)
      assert(res.value.head == all.head)
      assert(rest.isEmpty)
    }

    "parse examination phases" in {
      val input = "exam_phases: - exam_phase.none\n  - exam_phase.none"
      val (res, rest) = parser.parse(input)
      assert(res.value.size == 2)
      assert(res.value == NonEmptyList.of(all.head, all.head))
      assert(rest.isEmpty)
    }

    "parse examination phases raw" in {
      val input = "exam_phases: - exam_phase.none\n  - exam_phase.none"
      val (res, rest) = raw.parse(input)
      assert(res.value == NonEmptyList.of("none", "none"))
      assert(rest.isEmpty)
    }
  }
}
