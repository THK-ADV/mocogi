package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.metadata.CompetencesParser.competencesParser
import parsing.metadata.ECTSParser.ectsParser
import parsing.metadata.GlobalCriteriaParser.globalCriteriaParser
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.metadata.POParser.{mandatoryPOParser, optionalPOParser}
import parsing.metadata.ParticipantsParser.participantsParser
import parsing.metadata.PrerequisitesParser.{recommendedPrerequisitesParser, requiredPrerequisitesParser}
import parsing.metadata.WorkloadParser.workloadParser
import parsing.types._
import parsing.{intForKey, singleLineStringForKey}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
final class THKV1Parser @Inject() (
    responsibilitiesParser: ResponsibilitiesParser,
    seasonParser: SeasonParser,
    assessmentMethodParser: AssessmentMethodParser,
    statusParser: StatusParser,
    moduleTypeParser: ModuleTypeParser,
    locationParser: LocationParser,
    languageParser: LanguageParser
) extends MetadataParser {
  override val versionScheme = VersionScheme(1, "s")

  val idParser: Parser[UUID] =
    singleLineStringForKey("id")
      .flatMap(s => Try(UUID.fromString(s)).fold(_ => never("uuid"), always))

  val titleParser = singleLineStringForKey("title")

  val abbreviationParser = singleLineStringForKey("abbreviation")

  val durationParser = intForKey("duration")

  def parser(implicit
      locations: Seq[Location],
      languages: Seq[Language],
      status: Seq[Status],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      persons: Seq[Person],
      focusAreas: Seq[FocusArea],
      competences: Seq[Competence],
      globalCriteria: Seq[GlobalCriteria]
  ): Parser[Metadata] =
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
      .take(assessmentMethodParser.parser)
      .take(workloadParser)
      .skip(newline)
      .take(
        recommendedPrerequisitesParser
          .zip(requiredPrerequisitesParser)
          .skip(optional(newline))
      )
      .take(statusParser.parser)
      .take(locationParser.parser)
      .skip(optional(newline))
      .take(
        mandatoryPOParser
          .zip(optionalPOParser.option)
      )
      .take(
        participantsParser.option
          .skip(zeroOrMoreSpaces)
          .zip(competencesParser.option)
          .skip(zeroOrMoreSpaces)
          .take(globalCriteriaParser.option)
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
              assessments,
              workload,
              (recommendedPrerequisites, requiredPrerequisites),
              status,
              location,
              (mandatoryPo, optionalPo),
              (participants, competences, globalCriteria)
            ) =>
          Metadata(
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
            assessments,
            workload,
            recommendedPrerequisites,
            requiredPrerequisites,
            status,
            location,
            mandatoryPo,
            optionalPo.getOrElse(Nil),
            participants,
            competences.getOrElse(Nil),
            globalCriteria.getOrElse(Nil)
          )
      }
}
