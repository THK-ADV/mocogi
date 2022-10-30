package parsing.base

import basedata._
import parser.Parser
import parser.Parser._
import parser.ParserOps.{
  P0,
  P10,
  P11,
  P12,
  P13,
  P14,
  P15,
  P2,
  P3,
  P4,
  P5,
  P6,
  P7,
  P8,
  P9
}
import parsing._

import java.time.LocalDate

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

  def urlParser: Parser[(String, String)] =
    singleLineStringForKey("de_url")
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("en_url").option.map(_.getOrElse("")))

  def gradeParser(implicit grades: Seq[Grade]): Parser[Grade] =
    singleValueParser("grade", g => s"grade.${g.abbrev}")

  def programDirectorParser(implicit persons: Seq[Person]): Parser[Person] =
    singleValueParser("program_director", p => s"person.${p.id}")

  def accreditationUntilParser: Parser[LocalDate] =
    dateForKey("accreditation_until")

  def studyFormScopeParser: Parser[StudyFormScope] =
    prefix("-")
      .skip(zeroOrMoreSpaces)
      .take(posIntForKey("program_duration"))
      .skip(zeroOrMoreSpaces)
      .zip(posIntForKey("total_ECTS"))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("de_reason").option.map(_.getOrElse("")))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("en_reason").option.map(_.getOrElse("")))
      .map(StudyFormScope.tupled)

  def studyFormEntryParser(implicit
      studyForms: Seq[StudyFormType]
  ): Parser[StudyForm] =
    prefix("-")
      .skip(zeroOrMoreSpaces)
      .take(
        singleValueParser[StudyFormType]("type", t => s"study_form.${t.abbrev}")
      )
      .skip(zeroOrMoreSpaces)
      .zip(posIntForKey("workload_per_ects"))
      .skip(zeroOrMoreSpaces)
      .take(
        prefix("scope:")
          .skip(zeroOrMoreSpaces)
          .take(studyFormScopeParser.many())
      )
      .map(StudyForm.tupled)

  def studyFormParser(implicit
      studyForms: Seq[StudyFormType]
  ): Parser[List[StudyForm]] =
    prefix("study_form:")
      .skip(zeroOrMoreSpaces)
      .take(studyFormEntryParser.many(zeroOrMoreSpaces))

  def languageParser(implicit langs: Seq[Language]): Parser[List[Language]] =
    multipleValueParser("language_of_instruction", l => s"lang.${l.abbrev}", 1)

  def seasonParser(implicit seasons: Seq[Season]): Parser[List[Season]] =
    multipleValueParser("beginning_of_program", s => s"season.${s.abbrev}", 1)

  def campusParser(implicit locations: Seq[Location]): Parser[List[Location]] =
    multipleValueParser("campus", l => s"location.${l.abbrev}", 1)

  def restrictedAdmissionParser(): Parser[RestrictedAdmission] =
    prefix("restricted_admission:")
      .skip(zeroOrMoreSpaces)
      .skip(prefix("value:"))
      .skip(zeroOrMoreSpaces)
      .take(boolean)
      .skip(zeroOrMoreSpaces)
      .zip(singleLineStringForKey("de_reason").option.map(_.getOrElse("")))
      .skip(zeroOrMoreSpaces)
      .take(singleLineStringForKey("en_reason").option.map(_.getOrElse("")))
      .map(RestrictedAdmission.tupled)

  def fileParser(implicit
      grades: Seq[Grade],
      persons: Seq[Person],
      studyForms: Seq[StudyFormType],
      langs: Seq[Language],
      seasons: Seq[Season],
      locations: Seq[Location]
  ): Parser[List[StudyProgram]] =
    removeIndentation()
      .take(
        prefixTo(":")
          .skip(newline)
          .skip(zeroOrMoreSpaces)
          .zip(labelParser)
          .skip(zeroOrMoreSpaces)
          .take(abbreviationParser)
          .skip(zeroOrMoreSpaces)
          .take(urlParser)
          .skip(zeroOrMoreSpaces)
          .take(gradeParser)
          .skip(zeroOrMoreSpaces)
          .take(programDirectorParser)
          .skip(zeroOrMoreSpaces)
          .take(accreditationUntilParser)
          .skip(zeroOrMoreSpaces)
          .take(studyFormParser)
          .skip(zeroOrMoreSpaces)
          .take(languageParser)
          .skip(zeroOrMoreSpaces)
          .take(seasonParser)
          .skip(zeroOrMoreSpaces)
          .take(campusParser)
          .skip(zeroOrMoreSpaces)
          .take(restrictedAdmissionParser())
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("de_desc"))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("de_note"))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("en_desc"))
          .skip(zeroOrMoreSpaces)
          .take(stringForKey("en_note"))
          .map {
            case (
                  id,
                  label,
                  abbrev,
                  url,
                  grade,
                  director,
                  accreditation,
                  studyForm,
                  lang,
                  season,
                  campus,
                  admission,
                  deDesc,
                  deNote,
                  enDesc,
                  enNote
                ) =>
              basedata.StudyProgram(
                id,
                label._1,
                label._2,
                abbrev._1,
                abbrev._2,
                url._1,
                url._2,
                grade,
                director,
                accreditation,
                studyForm,
                lang,
                season,
                campus,
                admission,
                deDesc,
                deNote,
                enDesc,
                enNote
              )
          }
          .all(zeroOrMoreSpaces)
      )
}
