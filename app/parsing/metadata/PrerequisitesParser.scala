package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser
import parsing.types.Prerequisites
import parsing.{removeIndentation, stringForKey}

object PrerequisitesParser extends MultipleValueParser[String] {

  private def textParser =
    stringForKey("text").option
      .map(_.getOrElse(""))

  private def modulesParser =
    multipleParser(
      "modules",
      skipFirst(prefix("module.")).take(prefixTo("\n"))
    ).option.map(_.getOrElse(Nil))

  private def studyProgramsParser =
    multipleParser(
      "study_programs",
      skipFirst(prefix("study_program.")).take(prefixTo("\n"))
    ).option.map(_.getOrElse(Nil))

  private def parser(key: String): Parser[Option[Prerequisites]] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(removeIndentation())
      .take(textParser)
      .zip(modulesParser)
      .take(studyProgramsParser)
      .map(Prerequisites.tupled)
      .option

  val recommendedPrerequisitesParser: Parser[Option[Prerequisites]] =
    parser("recommended_prerequisites")

  val requiredPrerequisitesParser: Parser[Option[Prerequisites]] =
    parser("required_prerequisites")
}
