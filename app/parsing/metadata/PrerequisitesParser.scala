package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser
import parsing.{removeIndentation, stringForKey}
import parsing.types.Prerequisites

object PrerequisitesParser extends MultipleValueParser[String] {

  private def textParser =
    stringForKey("text")

  private def modulesParser =
    multipleParser(
      "modules",
      skipFirst(prefix("module."))
        .take(prefixTo("\n"))
    )

  private def studyProgramsParser =
    multipleParser(
      "study_programs",
      skipFirst(prefix("study_program."))
        .take(prefixTo("\n"))
    )

  private def parser(key: String): Parser[Option[Prerequisites]] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        oneOf(
          prefix("none")
            .skip(newline)
            .map(_ => None),
          skipFirst(removeIndentation())
            .take(textParser)
            .zip(modulesParser)
            .take(studyProgramsParser)
            .map(a => Some(Prerequisites(a._1, a._2, a._3)))
        )
      )

  val recommendedPrerequisitesParser: Parser[Option[Prerequisites]] =
    parser("recommended_prerequisites")

  val requiredPrerequisitesParser: Parser[Option[Prerequisites]] =
    parser("required_prerequisites")
}
