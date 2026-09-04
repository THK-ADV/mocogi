package printing.latex

import java.nio.file.Files
import java.nio.file.Path

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class TextIntroRewriterSpec extends AnyWordSpec with Matchers {

  private val pandocBaseline =
    Path.of("test/printing/latex/res/word_tabellen_pandoc_test.tex")

  private def rewrite(content: String): String = {
    val directory = Files.createTempDirectory("text-intro-rewriter")
    val source    = directory.resolve("source.tex")
    val target    = directory.resolve("intro_source.tex")
    try {
      Files.writeString(source, content)
      Files.readString(new TextIntroRewriter().rewrite(source).get)
    } finally {
      Files.deleteIfExists(target)
      Files.deleteIfExists(source)
      Files.deleteIfExists(directory)
    }
  }

  private def insertAfterOnce(tex: String, row: String, rule: String): String = {
    val anchor = s"\n$row\n"
    tex.sliding(anchor.length).count(_ == anchor) shouldBe 1
    tex.replace(anchor, s"$anchor$rule\n")
  }

  "TextIntroRewriter" should {
    "rewrite the Pandoc Word-table baseline exactly as expected" in {
      val baseline = Files.readString(pandocBaseline)
      val expected = List(
        """\end{minipage}} & B1 & C1 & D1 \\""" -> """\cmidrule(lr){2-4}""",
        """& B2 & C2 & D2 \\"""                 -> """\cmidrule(lr){2-4}""",
        """\end{minipage}} & D3 \\"""           -> """\cmidrule(lr){1-2}\cmidrule(lr){4-4}""",
        """\end{minipage}}} & C2 & D2 \\"""     -> """\cmidrule(lr){3-4}""",
        """& & C3 & D3 \\"""                    -> """\cmidrule(lr){1-4}""",
        """A1 & B1 & \multirow{3}{=}{\begin{minipage}[t]{\linewidth}\centering
          |\textbf{Rand\\
          |3 Zeilen}\strut
          |\end{minipage}} \\""".stripMargin -> """\cmidrule(lr){1-2}""",
        """A2 & B2 \\"""                     -> """\cmidrule(lr){1-2}"""
      ).foldLeft(baseline) {
        case (tex, (row, rule)) =>
          insertAfterOnce(tex, row, rule)
      }.stripSuffix("\n")

      rewrite(baseline) shouldBe expected
    }

    "leave content and longtables without multirow unchanged" in {
      val inputs = List(
        "plain text",
        """\begin{longtable}[]{ll}
          |\endlastfoot
          |A & B \\
          |C & D \\
          |\end{longtable}""".stripMargin,
        """\begin{longtable}[]{llll}
          |\endlastfoot
          |\multicolumn{2}{l}{AB} & \multicolumn{2}{l}{CD} \\
          |A & B & C & D \\
          |\end{longtable}""".stripMargin
      )

      inputs.foreach(input => rewrite(input) shouldBe input)
    }

    "remove numbered and unnumbered Pandoc caption prefixes" in {
      val input =
        """\caption{Tabelle : Studienverlaufsplan}
          |\caption{Abbildung 1.2: Übersicht}
          |\caption{Tabelle der Ergebnisse}
          |\caption{Tabelle 1x1 im Vergleich}
          |\caption{Abbildung 2024er Kohorte}
          |\caption{Tabelle: Hinweise zur Prüfung}""".stripMargin

      rewrite(input) shouldBe
        """\caption{Studienverlaufsplan}
          |\caption{Übersicht}
          |\caption{Tabelle der Ergebnisse}
          |\caption{Tabelle 1x1 im Vergleich}
          |\caption{Abbildung 2024er Kohorte}
          |\caption{Tabelle: Hinweise zur Prüfung}""".stripMargin
    }

    "keep an explicit line break and escaped ampersand inside a cell" in {
      val input =
        """\begin{longtable}[]{lll}
          |\endlastfoot
          |\begin{minipage}[t]{\linewidth}\raggedright
          |first\\
          |second\strut
          |\end{minipage} & A \& B & \multirow{2}{=}{right} \\
          |C & D \\
          |\end{longtable}""".stripMargin
      val expected = input.replace(
        """\end{minipage} & A \& B & \multirow{2}{=}{right} \\
          |C & D \\""".stripMargin,
        """\end{minipage} & A \& B & \multirow{2}{=}{right} \\
          |\cmidrule(lr){1-2}
          |C & D \\""".stripMargin
      )

      rewrite(input) shouldBe expected
    }

    "support multirow alignment and a span covering several columns" in {
      val input =
        """\begin{longtable}[]{llll}
          |\endlastfoot
          |\multicolumn{3}{l}{\multirow[t]{2}{*}{wide}} & A \\
          |& & & B \\
          |C & D & E & F \\
          |\end{longtable}""".stripMargin
      val expected = input
        .replace(
          """\multicolumn{3}{l}{\multirow[t]{2}{*}{wide}} & A \\
            |& & & B \\""".stripMargin,
          """\multicolumn{3}{l}{\multirow[t]{2}{*}{wide}} & A \\
            |\cmidrule(lr){4-4}
            |& & & B \\""".stripMargin
        )
        .replace(
          """& & & B \\
            |C & D & E & F \\""".stripMargin,
          """& & & B \\
            |\cmidrule(lr){1-4}
            |C & D & E & F \\""".stripMargin
        )

      rewrite(input) shouldBe expected
    }
  }
}
