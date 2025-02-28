package printing.html

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.view.StudyProgramViewRepository
import models.StudyProgramView
import parsing.types.Module
import printing.markdown.ModuleMarkdownPrinter
import printing.pandoc.PandocApi
import printing.pandoc.PrinterOutput
import printing.pandoc.PrinterOutputType
import printing.PrintingLanguage

@Singleton
final class ModuleHTMLPrinter @Inject() (
    studyProgramViewRepo: StudyProgramViewRepository,
    printer: ModuleMarkdownPrinter,
    pandoc: PandocApi,
    implicit val ctx: ExecutionContext
) {
  def print(
      module: Module,
      lang: PrintingLanguage,
      lastModified: LocalDateTime,
      outputType: PrinterOutputType,
      studyProgram: String => Option[StudyProgramView]
  ): Either[Throwable, PrinterOutput] = printer
    .printer(studyProgram)(lang, lastModified)
    .print(module, new StringBuilder())
    .flatMap(s => pandoc.run(module.metadata.id, outputType, s.toString(), lang))

  def print(
      module: Module,
      lang: PrintingLanguage,
      lastModified: LocalDateTime,
      outputType: PrinterOutputType
  ): Future[Either[Throwable, PrinterOutput]] =
    studyProgramViewRepo.all().map { sps =>
      print(
        module,
        lang,
        lastModified,
        outputType,
        sp => sps.find(_.id == sp)
      )
    }
}
