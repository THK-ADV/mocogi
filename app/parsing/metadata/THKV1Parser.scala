package parsing.metadata

import basedata._
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.metadata.AssessmentMethodParser.{
  assessmentMethodsMandatoryParser,
  assessmentMethodsOptionalParser
}
import parsing.metadata.CompetencesParser.competencesParser
import parsing.metadata.ECTSParser.ectsParser
import parsing.metadata.GlobalCriteriaParser.globalCriteriaParser
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.metadata.POParser.{mandatoryPOParser, optionalPOParser}
import parsing.metadata.ParticipantsParser.participantsParser
import parsing.metadata.PrerequisitesParser.{
  recommendedPrerequisitesParser,
  requiredPrerequisitesParser
}
import parsing.metadata.TaughtWithParser.taughtWithParser
import parsing.metadata.WorkloadParser.workloadParser
import parsing.types._
import parsing.{posIntForKey, singleLineStringForKey, uuidParser}

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
final class THKV1Parser @Inject() (
    responsibilitiesParser: ResponsibilitiesParser,
    seasonParser: SeasonParser,
    statusParser: StatusParser,
    moduleTypeParser: ModuleTypeParser,
    locationParser: LocationParser,
    languageParser: LanguageParser
) extends MetadataParser {
  override val versionScheme = VersionScheme(1, "s")

  val idParser: Parser[UUID] =
    singleLineStringForKey("id")
      .flatMap(uuidParser)

  val titleParser = singleLineStringForKey("title")

  val abbreviationParser = singleLineStringForKey("abbreviation")

  val durationParser = posIntForKey("duration")

  def parser(implicit
      locations: Seq[Location],
      languages: Seq[Language],
      status: Seq[Status],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      persons: Seq[Person],
      focusAreas: Seq[FocusAreaPreview],
      competences: Seq[Competence],
      globalCriteria: Seq[GlobalCriteria],
      pos: Seq[PO]
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
        assessmentMethodsMandatoryParser
          .zip(assessmentMethodsOptionalParser.option.map(_.getOrElse(Nil)))
          .map(AssessmentMethods.tupled)
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
        mandatoryPOParser
          .zip(optionalPOParser.option.map(_.getOrElse(Nil)))
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
