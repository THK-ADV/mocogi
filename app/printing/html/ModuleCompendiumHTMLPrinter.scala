package printing.html

import models.StudyProgramShort
import parsing.types.ModuleCompendium
import printing.PrintingLanguage
import printing.markdown.ModuleCompendiumMarkdownPrinter
import printing.pandoc.{PandocApi, PrinterOutput, PrinterOutputType}
import service.core.StudyProgramService

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleCompendiumHTMLPrinter @Inject() (
    studyProgramService: StudyProgramService,
    printer: ModuleCompendiumMarkdownPrinter,
    pandoc: PandocApi,
    implicit val ctx: ExecutionContext
) {
  def print(
      moduleCompendium: ModuleCompendium,
      lang: PrintingLanguage,
      localDateTime: Option[LocalDateTime],
      outputType: PrinterOutputType,
      studyProgram: String => Option[StudyProgramShort]
  ): Either[Throwable, PrinterOutput] = printer
    .printer(studyProgram)(lang, localDateTime)
    .print(moduleCompendium, new StringBuilder())
    .flatMap(s =>
      pandoc.run(moduleCompendium.metadata.id, outputType, s.toString(), lang)
    )

  def print(
      moduleCompendium: ModuleCompendium,
      lang: PrintingLanguage,
      localDateTime: Option[LocalDateTime],
      outputType: PrinterOutputType
  ): Future[Either[Throwable, PrinterOutput]] =
    studyProgramService.allShort().map { sps =>
      print(
        moduleCompendium,
        lang,
        localDateTime,
        outputType,
        sp => sps.find(_.id == sp)
      )
    }
}
