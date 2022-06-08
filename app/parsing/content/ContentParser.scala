package parsing.content

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.Content

object ContentParser {
  val contentParser: Parser[(Content, Content)] =
    prefix("## (de) Angestrebte Lernergebnisse:")
      .skip(newline)
      .take(prefixTo("## (en) Learning Outcome:"))
      .skip(newline)
      .zip(prefixTo("## (de) Modulinhalte:"))
      .skip(newline)
      .take(prefixTo("## (en) Module Content:"))
      .skip(newline)
      .take(prefixTo("## (de) Lehr- und Lernmethoden (Medienformen):"))
      .skip(newline)
      .take(prefixTo("## (en) Teaching and Learning Methods:"))
      .skip(newline)
      .take(prefixTo("## (de) Empfohlene Literatur:"))
      .skip(newline)
      .take(prefixTo("## (en) Recommended Reading:"))
      .skip(newline)
      .take(prefixTo("## (de) Besonderheiten:"))
      .skip(newline)
      .take(prefixTo("## (en) Particularities:"))
      .skip(optional(newline))
      .take(rest)
      .skip(end)
      .map {
        case (
              deOutcome,
              enOutcome,
              deContent,
              enContent,
              deMethods,
              enMethods,
              deReading,
              enReading,
              deParticularities,
              enParticularities
            ) =>
          (
            Content(
              deOutcome,
              deContent,
              deMethods,
              deReading,
              deParticularities
            ),
            Content(
              enOutcome,
              enContent,
              enMethods,
              enReading,
              enParticularities
            )
          )
      }
}
