package parsing.metadata

import javax.inject.Inject
import javax.inject.Singleton

import models.core._
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types._
import printer.Printer

@Singleton
final class MetadataCompositeParser @Inject() (
    val parsers: Set[MetadataParser]
) extends MetadataParser {

  private def versionSchemePrinter: Printer[VersionScheme] = {
    import printer.PrinterOps.P0
    Printer
      .prefix("v")
      .take(Printer.double)
      .zip(Printer.prefix(_ != '\n'))
      .contraMapSuccess(t => (t.number, t.label))
  }

  def parser(
      implicit locations: Seq[ModuleLocation],
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
    prefix("---")
      .take(VersionSchemeParser.parser)
      .skip(newline)
      .flatMap[ParsedMetadata] { scheme =>
        parsers.find(_.versionScheme == scheme) match {
          case Some(p) =>
            p.parser
          case None =>
            never(
              versionSchemePrinter
                .print(scheme, new StringBuilder("unknown version scheme "))
                .map(_.toString())
                .getOrElse(s"unknown version scheme $scheme")
            )
        }
      }
      .skip(prefix("---"))

  override val versionScheme = VersionScheme(0, "")
}
