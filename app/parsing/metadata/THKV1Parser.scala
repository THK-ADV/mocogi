package parsing.metadata

import parser.Parser
import parser.Parser.{always, never, newline, optional}
import parser.ParserOps._
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.metadata.POParser.mandatoryPOParser
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
      focusAreas: Seq[FocusArea]
  ): Parser[Metadata] =
    idParser
      .zip(titleParser)
      .take(abbreviationParser)
      .take(moduleTypeParser.parser)
      .take(moduleRelationParser)
      .take(ECTSParser.ectsParser)
      .skip(optional(newline))
      .take(languageParser.parser)
      .take(durationParser)
      .skip(newline)
      .take(seasonParser.parser)
      .take(responsibilitiesParser.parser)
      .take(assessmentMethodParser.parser)
      .take(workloadParser)
      .skip(newline)
      .take(recommendedPrerequisitesParser)
      .take(requiredPrerequisitesParser)
      .skip(optional(newline))
      .take(statusParser.parser)
      .take(locationParser.parser)
      .skip(optional(newline))
      .take(mandatoryPOParser)
      .map(Metadata.tupled)
}
