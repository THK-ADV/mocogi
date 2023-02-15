package printing

import org.scalatest.wordspec.AnyWordSpec
import parsing.types.Content
import printing.yaml.ContentMarkdownPrinter

final class ContentMarkdownPrinterSpec extends AnyWordSpec with PrinterSpec {

  val printer = new ContentMarkdownPrinter()

  "A Content Printer" should {
    "print learning outcome header" in {
      assert(
        run(
          printer.learningOutcomeHeader(PrintingLanguage.German)
        ) == "## (de) Angestrebte Lernergebnisse:"
      )
      assert(
        run(
          printer.learningOutcomeHeader(PrintingLanguage.English)
        ) == "## (en) Learning Outcome:"
      )
    }

    "print module content header" in {
      assert(
        run(
          printer.moduleContentHeader(PrintingLanguage.German)
        ) == "## (de) Modulinhalte:"
      )
      assert(
        run(
          printer.moduleContentHeader(PrintingLanguage.English)
        ) == "## (en) Module Content:"
      )
    }

    "print teaching and learning methods header" in {
      assert(
        run(
          printer.teachingAndLearningMethodsHeader(PrintingLanguage.German)
        ) == "## (de) Lehr- und Lernmethoden (Medienformen):"
      )
      assert(
        run(
          printer.teachingAndLearningMethodsHeader(PrintingLanguage.English)
        ) == "## (en) Teaching and Learning Methods:"
      )
    }

    "print recommended reading header" in {
      assert(
        run(
          printer.recommendedReadingHeader(PrintingLanguage.German)
        ) == "## (de) Empfohlene Literatur:"
      )
      assert(
        run(
          printer.recommendedReadingHeader(PrintingLanguage.English)
        ) == "## (en) Recommended Reading:"
      )
    }

    "print particularities header" in {
      assert(
        run(
          printer.particularitiesHeader(PrintingLanguage.German)
        ) == "## (de) Besonderheiten:"
      )
      assert(
        run(
          printer.particularitiesHeader(PrintingLanguage.English)
        ) == "## (en) Particularities:"
      )
    }

    "print learning outcome" in {
      val gerText = "\n- Klassen\n- Vererbung\n- Polymorphie\n"
      val gerRes =
        """## (de) Angestrebte Lernergebnisse:
          |
          |- Klassen
          |- Vererbung
          |- Polymorphie
          |""".stripMargin
      assert(
        run(printer.learningOutcome(PrintingLanguage.German, gerText)) == gerRes
      )
      val enText = "\n- Classes\n- Inheritance\n- Polymorphism\n"
      val enRes =
        """## (en) Learning Outcome:
          |
          |- Classes
          |- Inheritance
          |- Polymorphism
          |""".stripMargin
      assert(
        run(printer.learningOutcome(PrintingLanguage.English, enText)) == enRes
      )
    }

    "print" in {
      val de = Content(
        "\nProgrammieren lernen\n",
        "\n- Klassen\n- Vererbung\n- Polymorphie\n",
        "\nSlides, Whiteboard\n",
        "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n",
        "\nnichts\n"
      )
      val en = Content(
        "\nLearn to code\n",
        "\n- Classes\n- Inheritance\n- Polymorphism\n",
        "",
        "\nProgrammieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt\n",
        "\nnothing"
      )
      val res =
        """## (de) Angestrebte Lernergebnisse:
          |
          |Programmieren lernen
          |
          |## (en) Learning Outcome:
          |
          |Learn to code
          |
          |## (de) Modulinhalte:
          |
          |- Klassen
          |- Vererbung
          |- Polymorphie
          |
          |## (en) Module Content:
          |
          |- Classes
          |- Inheritance
          |- Polymorphism
          |
          |## (de) Lehr- und Lernmethoden (Medienformen):
          |
          |Slides, Whiteboard
          |
          |## (en) Teaching and Learning Methods:
          |
          |## (de) Empfohlene Literatur:
          |
          |Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt
          |
          |## (en) Recommended Reading:
          |
          |Programmieren lernen mit Kotlin. Kohls, Dobrynin, Leonhardt
          |
          |## (de) Besonderheiten:
          |
          |nichts
          |
          |## (en) Particularities:
          |
          |nothing""".stripMargin
      assert(printer.printer().print((de, en), "").value == res)
    }
  }
}
