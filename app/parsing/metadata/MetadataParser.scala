package parsing.metadata

import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.metadata.LanguageParser.languageParser
import parsing.metadata.ModuleRelationParser.moduleRelationParser
import parsing.metadata.POParser.poParser
import parsing.metadata.PrerequisitesParser.{
  recommendedPrerequisitesParser,
  requiredPrerequisitesParser
}
import parsing.metadata.WorkloadParser.workloadParser
import parsing.types.Metadata
import parsing.{doubleForKey, intForKey, stringForKey}
import printer.Printer

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.Try

trait MetadataParser {
  val moduleCodeParser: Parser[UUID]
  val moduleTitleParser: Parser[String]
  val moduleAbbrevParser: Parser[String]
  val creditPointsParser: Parser[Double]
  val durationParser: Parser[Int]
  val semesterParser: Parser[Int]
  val parser: Parser[Metadata]
}

@Singleton
class MetadataParserImpl @Inject() (
    responsibilitiesParser: ResponsibilitiesParser,
    seasonParser: SeasonParser,
    assessmentMethodParser: AssessmentMethodParser,
    statusParser: StatusParser,
    moduleTypeParser: ModuleTypeParser,
    locationParser: LocationParser
) extends MetadataParser {

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
      .take(moduleTypeParser.parser)
      .take(moduleRelationParser)
      .take(creditPointsParser)
      .skip(newline)
      .take(languageParser)
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

  val parser: Parser[Metadata] =
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
