package parsing.metadata

import models.core._
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types._

import java.util.UUID

object THKV1Parser {
  import parsing.{posIntForKey, singleLineStringForKey, uuidParser}

  def idParser: Parser[UUID] =
    singleLineStringForKey("id").flatMap(uuidParser)

  def titleParser = singleLineStringForKey("title")

  def abbreviationParser = singleLineStringForKey("abbreviation")

  def durationParser = posIntForKey("duration")
}

final class THKV1Parser extends MetadataParser {
  import THKV1Parser.{abbreviationParser, durationParser, idParser, titleParser}

  override val versionScheme = VersionScheme(1, "s")

  def parser(implicit
      locations: Seq[ModuleLocation],
      languages: Seq[ModuleLanguage],
      status: Seq[ModuleStatus],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      identities: Seq[Identity],
      focusAreas: Seq[FocusAreaID],
      competences: Seq[ModuleCompetence],
      globalCriteria: Seq[ModuleGlobalCriteria],
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
              workload,
              prerequisites,
              status,
              location,
              pos,
              (participants, competences, globalCriteria, taughtWith)
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
            workload,
            prerequisites,
            status,
            location,
            pos,
            participants,
            competences.getOrElse(Nil),
            globalCriteria.getOrElse(Nil),
            taughtWith
          )
      }
}
