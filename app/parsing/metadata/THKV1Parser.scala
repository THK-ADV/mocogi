package parsing.metadata

import models.core._
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.metadata.ModuleCompetencesParser.competencesParser
import parsing.metadata.ModuleECTSParser.ectsParser
import parsing.metadata.ModuleGlobalCriteriaParser.globalCriteriaParser
import parsing.metadata.ModuleParticipantsParser.participantsParser
import parsing.metadata.ModulePrerequisitesParser.{
  recommendedPrerequisitesParser,
  requiredPrerequisitesParser
}
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.metadata.ModuleTaughtWithParser.taughtWithParser
import parsing.metadata.ModuleWorkloadParser.workloadParser
import parsing.types._
import parsing.{posIntForKey, singleLineStringForKey, uuidParser}

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
final class THKV1Parser @Inject() (
    responsibilitiesParser: ModuleResponsibilitiesParser,
    seasonParser: ModuleSeasonParser,
    statusParser: ModuleStatusParser,
    moduleTypeParser: ModuleTypeParser,
    locationParser: ModuleLocationParser,
    languageParser: ModuleLanguageParser
) extends MetadataParser {
  override val versionScheme = VersionScheme(1, "s")

  val idParser: Parser[UUID] =
    singleLineStringForKey("id")
      .flatMap(uuidParser)

  val titleParser = singleLineStringForKey("title")

  val abbreviationParser = singleLineStringForKey("abbreviation")

  val durationParser = posIntForKey("duration")

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
      .take(moduleTypeParser.parser)
      .take(moduleRelationParser)
      .take(ectsParser)
      .skip(optional(newline))
      .take(languageParser.parser)
      .take(durationParser)
      .skip(newline)
      .take(seasonParser.parser)
      .take(responsibilitiesParser.parser)
      .take(
        ModuleAssessmentMethodParser.mandatoryParser
          .zip(
            ModuleAssessmentMethodParser.electiveParser.option.map(
              _.getOrElse(Nil)
            )
          )
          .map((ModuleAssessmentMethods.apply _).tupled)
      )
      .take(workloadParser)
      .skip(newline)
      .take(
        recommendedPrerequisitesParser.option
          .zip(requiredPrerequisitesParser.option)
          .skip(optional(newline))
          .map(ParsedPrerequisites.tupled)
      )
      .take(statusParser.parser)
      .take(locationParser.parser)
      .skip(optional(newline))
      .take(
        ModulePOParser.mandatoryParser.option
          .map(_.getOrElse(Nil))
          .zip(ModulePOParser.electiveParser.option.map(_.getOrElse(Nil)))
          .map(ParsedPOs.tupled)
      )
      .take(
        participantsParser.option
          .skip(zeroOrMoreSpaces)
          .zip(competencesParser.option)
          .skip(zeroOrMoreSpaces)
          .take(globalCriteriaParser.option)
          .skip(zeroOrMoreSpaces)
          .take(taughtWithParser.option.map(_.getOrElse(Nil)))
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
