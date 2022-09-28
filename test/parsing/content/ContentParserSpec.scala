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

      assert(de.learningOutcomeHeader == "Angestrebte Lernergebnisse")
      assert(de.learningOutcomeBody == "\n")
      assert(en.learningOutcomeHeader == "Learning Outcome")
      assert(en.learningOutcomeBody == "\n")

      assert(de.contentHeader == "Modulinhalte")
      assert(de.contentBody == "\n")
      assert(en.contentHeader == "Module Content")
      assert(en.contentBody == "\n")

      assert(
        de.teachingAndLearningMethodsHeader == "Lehr- und Lernmethoden (Medienformen)"
      )
      assert(de.teachingAndLearningMethodsBody == "\n")
      assert(
        en.teachingAndLearningMethodsHeader == "Teaching and Learning Methods"
      )
      assert(en.teachingAndLearningMethodsBody == "\n")

      assert(de.recommendedReadingHeader == "Empfohlene Literatur")
      assert(de.recommendedReadingBody == "\n")
      assert(en.recommendedReadingHeader == "Recommended Reading")
      assert(en.recommendedReadingBody == "\n")

      assert(de.particularitiesHeader == "Besonderheiten")
      assert(de.particularitiesBody == "\n")
      assert(en.particularitiesHeader == "Particularities")
      assert(en.particularitiesBody == "")
    }

    "parse content1.md" in {
      val (res, rest) =
        withFile0("test/parsing/res/content1.md")(contentParser.parse)
      val (de, en) = res.value

      assert(rest.isEmpty)

      assert(de.learningOutcomeHeader == "Angestrebte Lernergebnisse")
      assert(de.learningOutcomeBody == "\nProgrammieren lernen\n\n")
      assert(en.learningOutcomeHeader == "Learning Outcome")
      assert(en.learningOutcomeBody == "\nLearn to code\n\n")

      assert(de.contentHeader == "Modulinhalte")
      assert(de.contentBody == "\n- Klassen\n- Vererbung\n- Polymorphie\n\n")
      assert(en.contentHeader == "Module Content")
      assert(en.contentBody == "\n- Classes\n- Inheritance\n- Polymorphism\n\n")

      assert(
        de.teachingAndLearningMethodsHeader == "Lehr- und Lernmethoden (Medienformen)"
      )
      assert(de.teachingAndLearningMethodsBody == "\nSlides, Whiteboard\n\n")
      assert(
        en.teachingAndLearningMethodsHeader == "Teaching and Learning Methods"
      )
      assert(en.teachingAndLearningMethodsBody == "\n")

      assert(de.recommendedReadingHeader == "Empfohlene Literatur")
      assert(
        de.recommendedReadingBody == "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n\n"
      )
      assert(en.recommendedReadingHeader == "Recommended Reading")
      assert(
        en.recommendedReadingBody == "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n\n"
      )

      assert(de.particularitiesHeader == "Besonderheiten")
      assert(de.particularitiesBody == "\nnichts\n\n")
      assert(en.particularitiesHeader == "Particularities")
      assert(en.particularitiesBody == "\nnothing")
    }
  }
}
