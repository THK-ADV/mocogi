package parsing.metadata

import models.ModuleWorkload
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.metadata.ModuleWorkloadParser.parser
import parsing.ParserSpecHelper

class ModuleWorkloadParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues {

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
      val (res, rest) = parser.parse(input)
      assert(res.value == ModuleWorkload(36, 0, 18, 18, 0, 0))
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
      val (res, rest) = parser.parse(input)
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
