package parsing.metadata

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

  val versionSchemeParser: Parser[VersionScheme] =
    prefix("v")
      .take(double)
      .zip(prefix(_ != '\n'))
      .map(VersionScheme.tupled)

  val versionSchemePrinter: Printer[VersionScheme] = {
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
      focusAreas: Seq[FocusArea],
      competences: Seq[Competence],
      globalCriteria: Seq[GlobalCriteria],
      studyPrograms: Seq[StudyProgram]
  ): Parser[Metadata] =
    prefix("---")
      .take(versionSchemeParser)
      .skip(newline)
      .flatMap[Metadata] { scheme =>
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
