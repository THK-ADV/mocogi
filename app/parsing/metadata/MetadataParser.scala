package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.metadata.AssessmentMethodParser.assessmentMethodParser
import parsing.metadata.LanguageParser.languageParser
import parsing.metadata.LocationParser.locationParser
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.metadata.ModuleTypeParser.moduleTypeParser
import parsing.metadata.POParser.poParser
import parsing.metadata.PrerequisitesParser.{recommendedPrerequisitesParser, requiredPrerequisitesParser}
import parsing.metadata.ResponsibilitiesParser.responsibilitiesParser
import parsing.metadata.SeasonParser.seasonParser
import parsing.metadata.StatusParser.statusParser
import parsing.metadata.WorkloadParser.workloadParser
import parsing.types.Metadata
import parsing.{doubleForKey, intForKey, stringForKey}

import java.util.UUID
import scala.util.Try

object MetadataParser {

  val moduleCodeParser: Parser[UUID] =
    stringForKey("module_code")
      .flatMap(s => Try(UUID.fromString(s)).fold(_ => never("uuid"), always))

  val moduleTitleParser = stringForKey("module_title")

  val moduleAbbrevParser = stringForKey("module_abbrev")

  val creditPointsParser = doubleForKey("credit_points")

  val durationParser = intForKey("duration_of_module")

  val semesterParser = intForKey("recommended_semester")

  val metadataV1Parser: Parser[Metadata] =
    prefix("---")
      .skip(newline)
      .take(moduleCodeParser)
      .zip(moduleTitleParser)
      .take(moduleAbbrevParser)
      .take(moduleTypeParser)
      .take(moduleRelationParser)
      .take(creditPointsParser)
      .skip(newline)
      .take(languageParser)
      .take(durationParser)
      .skip(newline)
      .take(semesterParser)
      .skip(newline)
      .take(seasonParser)
      .take(responsibilitiesParser)
      .take(assessmentMethodParser)
      .take(workloadParser)
      .take(recommendedPrerequisitesParser)
      .take(requiredPrerequisitesParser)
      .take(statusParser)
      .take(locationParser)
      .take(poParser)
      .skip(prefix("---"))
      .map(Metadata.tupled)
}
