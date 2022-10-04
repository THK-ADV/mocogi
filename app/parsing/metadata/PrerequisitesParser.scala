package parsing.metadata

import basedata.PO
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types.ParsedPrerequisiteEntry
import parsing.{multipleValueParser, removeIndentation, stringForKey}

object PrerequisitesParser {

  private def textParser: Parser[String] =
    stringForKey("text").option
      .map(_.getOrElse(""))

  private def modulesParser: Parser[List[String]] =
    multipleValueParser(
      "modules",
      skipFirst(prefix("module.")).take(prefixTo("\n"))
    ).option.map(_.getOrElse(Nil))

  private def studyProgramsParser(implicit pos: Seq[PO]): Parser[List[PO]] =
    multipleValueParser(
      "study_programs",
      oneOf(
        pos.map(s =>
          literal(s"study_program.${s.abbrev}")
            .map(_ => s)
        ): _*
      )
    ).option.map(_.getOrElse(Nil))

  private def parser(
      key: String
  )(implicit pos: Seq[PO]): Parser[ParsedPrerequisiteEntry] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(removeIndentation())
      .take(textParser)
      .zip(modulesParser)
      .take(studyProgramsParser)
      .map(ParsedPrerequisiteEntry.tupled)

  def recommendedPrerequisitesParser(implicit
      pos: Seq[PO]
  ): Parser[ParsedPrerequisiteEntry] =
    parser("recommended_prerequisites")

  def requiredPrerequisitesParser(implicit
      pos: Seq[PO]
  ): Parser[ParsedPrerequisiteEntry] =
    parser("required_prerequisites")
}
