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

@deprecated("Gets replaced by JSON representation")
@Singleton
final class ModuleHTMLPrinter @Inject() (
    studyProgramViewRepo: StudyProgramViewRepository,
    printer: ModuleMarkdownPrinter,
    pandoc: PandocApi,
    implicit val ctx: ExecutionContext
) {
  def print(
      module: Module,
      lastModified: LocalDateTime,
      outputType: PrinterOutputType,
      studyProgram: String => Option[StudyProgramView]
  ): Either[Throwable, PrinterOutput] = printer
    .printer(studyProgram)(lastModified)
    .print(module, new StringBuilder())
    .flatMap(s => pandoc.run(module.metadata.id, outputType, s.toString()))

  def print(
      module: Module,
      lastModified: LocalDateTime,
      outputType: PrinterOutputType
  ): Future[Either[Throwable, PrinterOutput]] =
    studyProgramViewRepo.notExpired().map { sps =>
      print(
        module,
        lastModified,
        outputType,
        sp => sps.find(_.id == sp)
      )
    }
}
