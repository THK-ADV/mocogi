package parsing.metadata

import java.util.UUID

import models.core.*
import models.core.ExamPhases.ExamPhase
import parser.Parser
import parser.Parser.*
import parser.ParserOps.*
import parsing.types.*

object THKV1Parser {
  import parsing.posIntForKey
  import parsing.singleLineStringForKey
  import parsing.uuidParser

  def idKey = "id"

  def titleKey = "title"

  def abbreviationKey = "abbreviation"

  def durationKey = "duration"

  def idParser: Parser[UUID] =
    singleLineStringForKey(idKey).flatMap(uuidParser)

  def titleParser = singleLineStringForKey(titleKey)

  def abbreviationParser = singleLineStringForKey(abbreviationKey)

  def durationParser = posIntForKey(durationKey)
}

final class THKV1Parser extends MetadataParser {
  import THKV1Parser.abbreviationParser
  import THKV1Parser.durationParser
  import THKV1Parser.idParser
  import THKV1Parser.titleParser

  override val versionScheme = VersionScheme.default

  // TODO replace with real data at some point
  implicit def allExamPhases: List[ExamPhase] = ExamPhase.all.toList

  def parser(
      implicit locations: Seq[ModuleLocation],
      languages: Seq[ModuleLanguage],
      status: Seq[ModuleStatus],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      identities: Seq[Identity],
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[ParsedMetadata] =
    idParser
      .zip(titleParser)
      .take(abbreviationParser)
      .take(ModuleTypeParser.parser)
      .take(ModuleRelationParser.parser)
      .take(ModuleECTSParser.parser)
      .skip(optional(newline))
      .take(ModuleLanguageParser.parser)
      .take(durationParser)
      .skip(newline)
      .take(ModuleSeasonParser.parser)
      .take(ModuleResponsibilitiesParser.parser)
      .take(ModuleAssessmentMethodParser.parser)
      .skip(zeroOrMoreSpaces)
      .take(
        ExaminerParser.parser
          .skip(zeroOrMoreSpaces)
          .zip(ExamPhaseParser.parser)
          .skip(zeroOrMoreSpaces)
      )
      .take(ModuleWorkloadParser.parser)
      .skip(newline)
      .take(ModulePrerequisitesParser.parser)
      .skip(zeroOrMoreSpaces)
      .take(ModuleStatusParser.parser)
      .take(ModuleLocationParser.parser)
      .skip(optional(newline))
      .take(ModulePOParser.parser)
      .take(
        ModuleParticipantsParser.parser.option
          .skip(zeroOrMoreSpaces)
          .zip(ModuleCompetencesParser.parser.option)
          .skip(zeroOrMoreSpaces)
          .take(ModuleGlobalCriteriaParser.parser.option)
          .skip(zeroOrMoreSpaces)
          .take(ModuleTaughtWithParser.parser.option.map(_.getOrElse(Nil)))
          .skip(zeroOrMoreSpaces)
          .take(AttendanceRequirementParser.parser.option)
          .skip(zeroOrMoreSpaces)
          .take(AssessmentPrerequisiteParser.parser.option)
          .skip(zeroOrMoreSpaces)
      )
      .map {
        case (
              id,
              title,
              abbrev,
              moduleType,
              relation,
              ects,
              lang,
              duration,
              season,
              resp,
              assessmentMethods,
              (examiner, examPhases),
              workload,
              prerequisites,
              status,
              location,
              pos,
              (participants, competences, globalCriteria, taughtWith, attReq, assPre)
            ) =>
          ParsedMetadata(
            id,
            title,
            abbrev,
            moduleType,
            relation,
            ects,
            lang,
            duration,
            season,
            resp,
            assessmentMethods,
            examiner,
            examPhases,
            workload,
            prerequisites,
            status,
            location,
            pos,
            participants,
            competences.getOrElse(Nil),
            globalCriteria.getOrElse(Nil),
            taughtWith,
            attReq,
            assPre
          )
      }
}
