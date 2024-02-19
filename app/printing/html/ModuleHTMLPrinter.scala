package printing.html

import database.view.StudyProgramViewRepository
import models.StudyProgramView
import parsing.types.Module
import printing.PrintingLanguage
import printing.markdown.ModuleMarkdownPrinter
import printing.pandoc.{PandocApi, PrinterOutput, PrinterOutputType}

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
      localDateTime: Option[LocalDateTime],
      outputType: PrinterOutputType,
      studyProgram: String => Option[StudyProgramView]
  ): Either[Throwable, PrinterOutput] = printer
    .printer(studyProgram)(lang, localDateTime)
    .print(module, new StringBuilder())
    .flatMap(s =>
      pandoc.run(module.metadata.id, outputType, s.toString(), lang)
    )

  def print(
      module: Module,
      lang: PrintingLanguage,
      localDateTime: Option[LocalDateTime],
      outputType: PrinterOutputType
  ): Future[Either[Throwable, PrinterOutput]] =
    studyProgramViewRepo.all().map { sps =>
      print(
        module,
        lang,
        localDateTime,
        outputType,
        sp => sps.find(_.id == sp)
      )
    }
}
