package parsing.metadata

import parser.Parser
import parser.Parser.{always, never, newline, optional}
import parser.ParserOps._
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.metadata.POParser.poParser
import parsing.metadata.PrerequisitesParser.{
  recommendedPrerequisitesParser,
  requiredPrerequisitesParser
}
import parsing.metadata.WorkloadParser.workloadParser
import parsing.types._
import parsing.{doubleForKey, intForKey, stringForKey}

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

  val moduleCodeParser: Parser[UUID] =
    stringForKey("module_code")
      .flatMap(s => Try(UUID.fromString(s)).fold(_ => never("uuid"), always))

  val moduleTitleParser = stringForKey("module_title")

  val moduleAbbrevParser = stringForKey("module_abbrev")

  val creditPointsParser = doubleForKey("credit_points")

  val durationParser = intForKey("duration_of_module")

  val semesterParser = intForKey("recommended_semester")

  def parser(implicit
      locations: Seq[Location],
      languages: Seq[Language],
      status: Seq[Status],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      persons: Seq[Person]
  ): Parser[Metadata] =
    moduleCodeParser
      .zip(moduleTitleParser)
      .take(moduleAbbrevParser)
      .take(moduleTypeParser.parser)
      .take(moduleRelationParser)
      .take(creditPointsParser)
      .skip(newline)
      .take(languageParser.parser)
      .take(durationParser)
      .skip(newline)
      .take(semesterParser)
      .skip(newline)
      .take(seasonParser.parser)
      .take(responsibilitiesParser.parser)
      .take(assessmentMethodParser.parser)
      .take(workloadParser)
      .take(recommendedPrerequisitesParser)
      .take(requiredPrerequisitesParser)
      .skip(optional(newline))
      .take(statusParser.parser)
      .take(locationParser.parser)
      .skip(optional(newline))
      .take(poParser)
      .map(Metadata.tupled)
}
