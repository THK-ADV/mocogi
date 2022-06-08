package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.WorkloadParser.workloadParser
import parsing.types.Workload

class WorkloadParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Workload Parser" should {
    "return a valid workload" in {
      val input =
        """workload: 150
          |  lecture: 36
          |  seminar: 0
          |  practical: 18
          |  exercise: 18
          |  self_study: 78
          |""".stripMargin
      val (res, rest) = workloadParser.run(input)
      assert(res.value == Workload(150, 36, 0, 18, 18, 78))
      assert(rest.isEmpty)
    }

    "fail if the numbers do not match" in {
      val input =
        """workload: 150
          |  lecture: 36
          |  seminar: 10
          |  practical: 18
          |  exercise: 18
          |  self_study: 78
          |""".stripMargin
      val (res, rest) = workloadParser.run(input)
      res match {
        case Right(_) => fail()
        case Left(e) =>
          assert(e.expected == s"total of workload to be 150, but was 160")
          assert(e.remainingInput == input)
          assert(rest == input)
      }
    }

    "fail if one entry is missing" in {
      val input =
        """workload: 150
          |  lecture: 36
          |  seminar: 10
          |  labwork: 18
          |  exercise: 18
          |  self_study: 78
          |""".stripMargin
      val (res, rest) = workloadParser.run(input)
      res match {
        case Right(_) => fail()
        case Left(e) =>
          assert(e.expected == "practical:")
          assert(e.remainingInput == input)
          assert(rest == input)
      }
    }
  }
}
