package parsing.metadata

import basedata.StudyProgram
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.helper.MultipleValueParser.multipleParser
import parsing.types.{POMandatory, POOptional}

object POParser {
  private def studyProgramParser(implicit
      studyPrograms: Seq[StudyProgram]
  ): Parser[StudyProgram] =
    prefix("- study_program:")
      .skip(zeroOrMoreSpaces)
      .take(
        oneOf(
          studyPrograms.map(s =>
            prefix(s"study_program.${s.abbrev}")
              .map(_ => s)
          ): _*
        )
      )

  private def recommendedSemesterParser =
    multipleParser("recommended_semester", int)

  private def recommendedSemesterPartTimeParser =
    multipleParser("recommended_semester_part_time", int).option
      .map(_.getOrElse(Nil))

  private def instanceOfParser =
    prefix("instance_of:")
      .skip(zeroOrMoreSpaces)
      .take(prefix("module.").take(prefixTo("\n").or(rest)))

  private def partOfCatalogParser =
    prefix("part_of_catalog:")
      .skip(zeroOrMoreSpaces)
      .take(boolean)

  def mandatoryPOParser(implicit
      studyPrograms: Seq[StudyProgram]
  ): Parser[List[POMandatory]] =
    prefix("po_mandatory:")
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser
          .skip(zeroOrMoreSpaces)
          .zip(recommendedSemesterParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterPartTimeParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(POMandatory.tupled))
      )

  def optionalPOParser(implicit
      studyPrograms: Seq[StudyProgram]
  ): Parser[List[POOptional]] =
    prefix("po_optional:")
      .skip(zeroOrMoreSpaces)
      .take(
        studyProgramParser
          .skip(zeroOrMoreSpaces)
          .zip(instanceOfParser)
          .skip(zeroOrMoreSpaces)
          .take(partOfCatalogParser)
          .skip(zeroOrMoreSpaces)
          .take(recommendedSemesterParser)
          .many(zeroOrMoreSpaces)
          .map(_.map(POOptional.tupled))
      )
}
