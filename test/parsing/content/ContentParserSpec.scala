package parsing.content

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.content.ContentParser.contentParser
import parsing.{ParserSpecHelper, withFile0}

class ContentParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Content Parser" should {

    "parse content2.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/content2.md")(contentParser.parse)
      val (de, en) = res.value

      assert(rest.isEmpty)

      assert(de.learningOutcome == "\n")
      assert(en.learningOutcome == "\n")
      assert(de.content == "\n")
      assert(en.content == "\n")
      assert(de.teachingAndLearningMethods == "\n")
      assert(en.teachingAndLearningMethods == "\n")
      assert(de.recommendedReading == "\n")
      assert(en.recommendedReading == "\n")
      assert(de.particularities == "\n")
      assert(en.particularities == "")
    }

    "parse content1.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/content1.md")(contentParser.parse)
      val (de, en) = res.value

      assert(rest.isEmpty)

      assert(de.learningOutcome == "\nProgrammieren lernen\n\n")
      assert(en.learningOutcome == "\nLearn to code\n\n")
      assert(de.content == "\n- Klassen\n- Vererbung\n- Polymorphie\n\n")
      assert(en.content == "\n- Classes\n- Inheritance\n- Polymorphism\n\n")
      assert(de.teachingAndLearningMethods == "\nSlides, Whiteboard\n\n")
      assert(en.teachingAndLearningMethods == "\n")
      assert(
        de.recommendedReading == "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n\n"
      )
      assert(
        en.recommendedReading == "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n\n"
      )
      assert(de.particularities == "\nnichts\n\n")
      assert(en.particularities == "\nnothing")
    }
  }
}
