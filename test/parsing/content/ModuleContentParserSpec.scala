package parsing.content

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.content.ModuleContentParser.contentParser
import parsing.{ParserSpecHelper, withFile0}

class ModuleContentParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Module Content Parser" should {

    "parse content2.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/content2.md")(contentParser.parse)
      val (de, en) = res.value

      assert(rest.isEmpty)

      assert(de.learningOutcome == "")
      assert(en.learningOutcome == "")
      assert(de.content == "")
      assert(en.content == "")
      assert(de.teachingAndLearningMethods == "")
      assert(en.teachingAndLearningMethods == "")
      assert(de.recommendedReading == "")
      assert(en.recommendedReading == "")
      assert(de.particularities == "")
      assert(en.particularities == "")
    }

    "parse content1.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/content1.md")(contentParser.parse)
      val (de, en) = res.value

      assert(rest.isEmpty)

      assert(de.learningOutcome == "Programmieren lernen")
      assert(en.learningOutcome == "Learn to code")
      assert(de.content == "- Klassen\n- Vererbung\n- Polymorphie")
      assert(en.content == "- Classes\n- Inheritance\n- Polymorphism")
      assert(de.teachingAndLearningMethods == "Slides, Whiteboard")
      assert(en.teachingAndLearningMethods == "")
      assert(
        de.recommendedReading == "Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt"
      )
      assert(
        en.recommendedReading == "Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt"
      )
      assert(de.particularities == "nichts")
      assert(en.particularities == "nothing")
    }
  }
}
