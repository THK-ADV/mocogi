package parsing.content

import parser.Parser._
import parser.ParserOps._
import parsing.types.ModuleContent

object ModuleContentParser {
  def contentParser =
    prefix("## (de)")
      .take(
        zeroOrMoreSpaces
          .skip(prefix("Angestrebte Lernergebnisse"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .skip(prefix("Learning Outcome"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .skip(prefix("Modulinhalte"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .skip(prefix("Module Content"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .skip(prefix("Lehr- und Lernmethoden (Medienformen)"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .skip(prefix("Teaching and Learning Methods"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .skip(prefix("Empfohlene Literatur"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .skip(prefix("Recommended Reading"))
          .skip(prefix(":"))
          .skip(newline)
          .zip(prefixTo("## (de)"))
      )
      .take(
        zeroOrMoreSpaces
          .skip(prefix("Besonderheiten"))
          .skip(prefix(":"))
          .skip(newline)
          .take(prefixTo("## (en)"))
          .skip(zeroOrMoreSpaces)
          .skip(prefix("Particularities"))
          .skip(prefix(":"))
          .skip(optional(newline))
          .zip(rest)
          .skip(end)
      )
      .map {
        case (
              deC0,
              enC0,
              (deC1, enC1),
              (deC2, enC2),
              (deC3, enC3),
              (deC4, enC4)
            ) =>
          (
            ModuleContent(
              deC0,
              deC1,
              deC2,
              deC3,
              deC4
            ),
            ModuleContent(
              enC0,
              enC1,
              enC2,
              enC3,
              enC4
            )
          )
      }
}
