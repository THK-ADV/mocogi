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
import parsing.metadata.PrerequisitesParser.{
  recommendedPrerequisitesParser,
  requiredPrerequisitesParser
}
import parsing.metadata.ResponsibilitiesParser.responsibilitiesParser
import parsing.metadata.SeasonParser.seasonParser
import parsing.metadata.StatusParser.statusParser
import parsing.metadata.WorkloadParser.workloadParser
import parsing.types.Metadata
import parsing.{doubleForKey, intForKey, stringForKey}
import printer.Printer

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

  val thkV1Parser: Parser[Metadata] =
    moduleCodeParser
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
      .skip(optional(newline))
      .take(statusParser)
      .take(locationParser)
      .skip(optional(newline))
      .take(poParser)
      .map(Metadata.tupled)

  val versionSchemeParser: Parser[(Double, String)] =
    prefix("v")
      .take(double)
      .zip(prefixUntil("\n"))

  val versionSchemePrinter: Printer[(Double, String)] = {
    import printer.PrinterOps.P0
    Printer
      .prefix("v")
      .take(Printer.double)
      .zip(Printer.prefix(_ != '\n'))
  }

  val metadataParser: Parser[Metadata] =
    prefix("---")
      .take(versionSchemeParser)
      .skip(newline)
      .flatMap[Metadata] {
        case (1, "s") => thkV1Parser
        case other =>
          never(
            versionSchemePrinter
              .print(other, "unknown version scheme ")
              .getOrElse(s"unknown version scheme $other")
          )
      }
      .skip(prefix("---"))
}
