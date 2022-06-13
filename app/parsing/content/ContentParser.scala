package parsing.content

import parser.Parser._
import parser.ParserOps._
import parsing.types.Content

object ContentParser {
  val contentParser =
    prefix("## (de)")
      .skip(zeroOrMoreSpaces)
      .take(literal("Sonstige empfohlene Voraussetzungen"))
      .skip(prefix(":"))
      .skip(newline)
      .zip(prefixTo("## (en)"))
      .skip(zeroOrMoreSpaces)
      .take(literal("Other recommended prerequisites"))
      .skip(prefix(":"))
      .skip(newline)
      .take(prefixTo("## (de)"))
      .take(
        zeroOrMoreSpaces
          .take(literal("Angestrebte Lernergebnisse"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .take(literal("Learning Outcome"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .take(literal("Modulinhalte"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .take(literal("Module Content"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .take(literal("Lehr- und Lernmethoden (Medienformen)"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .take(literal("Teaching and Learning Methods"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .take(literal("Empfohlene Literatur"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .take(literal("Recommended Reading"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .take(literal("Besonderheiten"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .take(literal("Particularities"))
          .skip(prefix(":"))
          .skip(optional(newline))
          .take(rest)
          .skip(end)
      )
      .map {
        case (
              deH1,
              deC1,
              enH1,
              enC1,
              (deH2, deC2, enH2, enC2),
              (deH3, deC3, enH3, enC3),
              (deH4, deC4, enH4, enC4),
              (deH5, deC5, enH5, enC5),
              (deH6, deC6, enH6, enC6)
            ) =>
          (
            Content(
              deH1,
              deC1,
              deH2,
              deC2,
              deH3,
              deC3,
              deH4,
              deC4,
              deH5,
              deC5,
              deH6,
              deC6
            ),
            Content(
              enH1,
              enC1,
              enH2,
              enC2,
              enH3,
              enC3,
              enH4,
              enC4,
              enH5,
              enC5,
              enH6,
              enC6
            )
          )
      }
}
