package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ModuleWorkloadParser.workloadParser
import parsing.types.ParsedWorkload

class ModuleWorkloadParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Workload Parser" should {
    "return a valid workload" in {
      val input =
        """workload:
          |  lecture: 36
          |  seminar: 0
          |  practical: 18
          |  exercise: 18
          |  project_supervision: 0
          |  project_work: 0""".stripMargin
      val (res, rest) = workloadParser.parse(input)
      assert(res.value == ParsedWorkload(36, 0, 18, 18, 0, 0))
      assert(rest.isEmpty)
    }

    "fail if one entry is missing" in {
      val input =
        """workload:
          |  lecture: 36
          |  seminar: 10
          |  labwork: 18
          |  exercise: 18
          |  project_supervision: 0
          |  project_work: 0""".stripMargin
      val (res, rest) = workloadParser.parse(input)
      res match {
        case Right(_) => fail()
        case Left(e) =>
          assert(e.expected == "practical:")
          assert(
            e.found ==
              """labwork: 18
              |  exercise: 18
              |  project_supervision: 0
              |  project_work: 0""".stripMargin
          )
          assert(rest == input)
      }
    }
  }
}
