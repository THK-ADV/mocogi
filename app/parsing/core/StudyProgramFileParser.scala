package parsing.core

import cats.data.NonEmptyList
import models.core._
import parser.Parser
import parser.Parser._
import parser.ParserOps.{P0, P2, P3, P4, P5}
import parsing._

object StudyProgramFileParser {

  def labelParser: Parser[(String, String)] =
    singleLineStringForKey("de_label")
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("en_label"))

  def abbreviationParser: Parser[(String, String)] =
    prefix("abbreviation:")
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("internal").option.map(_.getOrElse("")))
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("external").option.map(_.getOrElse("")))
      .option
      .map(_.getOrElse(("", "")))

  def degreeParser(implicit degrees: Seq[Degree]): Parser[Degree] =
    singleValueParser("grade", g => s"grade.${g.id}")

  def programDirectorParser(implicit
      persons: Seq[Identity]
  ): Parser[NonEmptyList[Identity]] =
    multipleValueParser(
      "program_director",
      (p: Identity) => s"person.${p.id}"
    )
      .nel()

  def examDirectorParser(implicit
      persons: Seq[Identity]
  ): Parser[NonEmptyList[Identity]] =
    multipleValueParser(
      "exam_director",
      (p: Identity) => s"person.${p.id}"
    ).nel()

  def fileParser(implicit
      degrees: Seq[Degree],
      persons: Seq[Identity]
  ): Parser[List[StudyProgram]] =
    removeIndentation()
      .take(
        prefixTo(":")
          .skip(zeroOrMoreSpaces)
          .zip(labelParser)
          .skip(zeroOrMoreSpaces)
          .take(abbreviationParser)
          .skip(zeroOrMoreSpaces)
          .skip(range("de_url:", "grade:"))
          .skip(zeroOrMoreSpaces)
          .take(degreeParser)
          .skip(zeroOrMoreSpaces)
          .take(programDirectorParser)
          .skip(zeroOrMoreSpaces)
          .take(examDirectorParser)
          .skip(zeroOrMoreSpaces)
          .skip(range("accreditation_until:", "de_desc:"))
          .skip(stringForKey("de_desc"))
          .skip(zeroOrMoreSpaces)
          .skip(stringForKey("de_note"))
          .skip(zeroOrMoreSpaces)
          .skip(stringForKey("en_desc"))
          .skip(zeroOrMoreSpaces)
          .skip(stringForKey("en_note"))
          .map { case (id, labels, abbrevs, degree, programDirs, examDirs) =>
            StudyProgram(
              id,
              labels._1,
              labels._2,
              abbrevs._1,
              abbrevs._2,
              degree.id,
              programDirs.map(_.id),
              examDirs.map(_.id)
            )
          }
          .all(zeroOrMoreSpaces)
      )
}
