package parsing.metadata

import models.core._
import parser.Parser
import parser.Parser._
import parser.ParserOps._
import parsing.types._
import printer.Printer

import javax.inject.{Inject, Singleton}

@Singleton
class MetadataCompositeParser @Inject() (
    val parsers: Set[MetadataParser]
) extends MetadataParser {

  private val versionSchemePrinter: Printer[VersionScheme] = {
    import printer.PrinterOps.P0
    Printer
      .prefix("v")
      .take(Printer.double)
      .zip(Printer.prefix(_ != '\n'))
      .contraMapSuccess(t => (t.number, t.label))
  }

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
                .print(scheme, "unknown version scheme ")
                .getOrElse(s"unknown version scheme $scheme")
            )
        }
      }
      .skip(prefix("---"))

  override val versionScheme = VersionScheme(0, "")
}
