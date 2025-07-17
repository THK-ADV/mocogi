package parsing.metadata

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues

final class AssessmentPrerequisiteParserSpec extends AnyWordSpec with EitherValues {

  import AssessmentPrerequisiteParser.parser

  "An AssessmentPrerequisiteParser" should {
    "parse an entry" in {
      val input = """assessment_prerequisite:
                    |  modules: Module...
                    |  reason: Hier Begründung""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(rest.isEmpty)
      assert(res.value.modules == "Module...")
      assert(res.value.reason == "Hier Begründung")
    }
  }
}
