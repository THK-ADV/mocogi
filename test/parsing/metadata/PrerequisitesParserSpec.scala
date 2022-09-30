package parsing.metadata

import basedata.StudyProgram
import helper.FakeStudyPrograms
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.PrerequisitesParser.recommendedPrerequisitesParser
import parsing.types.PrerequisiteEntry

class PrerequisitesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with FakeStudyPrograms {

  "A Prerequisites Parser" should {
    "parse prerequisites" in {
      val input =
        """recommended_prerequisites:
          |  text: >
          |    benötigt werden kenntnisse in algebra und java
          |
          |    und ein pc.
          |  modules:
          |    - module.ap1
          |    - module.ap2
          |  study_programs:
          |    - study_program.mi4""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == PrerequisiteEntry(
          "benötigt werden kenntnisse in algebra und java\nund ein pc.\n",
          List("ap1", "ap2"),
          List(StudyProgram("mi4"))
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text and no study programs" in {
      val input =
        """recommended_prerequisites:
          |  modules:
          |    - module.ap1
          |    - module.ap2
          |  """.stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == PrerequisiteEntry(
          "",
          List("ap1", "ap2"),
          Nil
        )
      )
      assert(rest.isEmpty)
    }

    "parse prerequisites with not text and no modules" in {
      val input =
        """recommended_prerequisites:
          |  study_programs:
          |    - study_program.mi4""".stripMargin
      val (res, rest) = recommendedPrerequisitesParser.parse(input)
      assert(
        res.value == PrerequisiteEntry(
          "",
          Nil,
          List(StudyProgram("mi4"))
        )
      )
      assert(rest.isEmpty)
    }
  }
}
