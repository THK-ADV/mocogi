package parsing.metadata

import basedata.StudyProgram
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser.multipleParser
import parsing.types.ParsedPrerequisiteEntry
import parsing.{removeIndentation, stringForKey}

object PrerequisitesParser {

  private def textParser: Parser[String] =
    stringForKey("text").option
      .map(_.getOrElse(""))

  private def modulesParser: Parser[List[String]] =
    multipleParser(
      "modules",
      skipFirst(prefix("module.")).take(prefixTo("\n"))
    ).option.map(_.getOrElse(Nil))

  private def studyProgramsParser(implicit
      studyPrograms: Seq[StudyProgram]
  ): Parser[List[StudyProgram]] =
    multipleParser(
      "study_programs",
      oneOf(
        studyPrograms.map(s =>
          literal(s"study_program.${s.abbrev}")
            .map(_ => s)
        ): _*
      )
    ).option.map(_.getOrElse(Nil))

  private def parser(
      key: String
  )(implicit studyPrograms: Seq[StudyProgram]): Parser[ParsedPrerequisiteEntry] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(removeIndentation())
      .take(textParser)
      .zip(modulesParser)
      .take(studyProgramsParser)
      .map(ParsedPrerequisiteEntry.tupled)

  def recommendedPrerequisitesParser(implicit
      studyPrograms: Seq[StudyProgram]
  ): Parser[ParsedPrerequisiteEntry] =
    parser("recommended_prerequisites")

  def requiredPrerequisitesParser(implicit
      studyPrograms: Seq[StudyProgram]
  ): Parser[ParsedPrerequisiteEntry] =
    parser("required_prerequisites")
}
