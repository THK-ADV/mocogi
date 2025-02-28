package git.subscriber

import java.time.LocalDateTime
import java.util.UUID

import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import database.view.StudyProgramViewRepository
import git.subscriber.ModuleSubscribers.Handle
import models.StudyProgramView
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.Props
import parsing.types.Module
import play.api.Logging
import printing.html.ModuleHTMLPrinter
import printing.pandoc.PrinterOutput
import printing.pandoc.PrinterOutputType
import printing.PrintingLanguage

object ModulePrintingActor {
  def props(
      printer: ModuleHTMLPrinter,
      outputType: PrinterOutputType,
      studyProgramViewRepo: StudyProgramViewRepository,
      ctx: ExecutionContext
  ) =
    Props(
      new Impl(
        printer,
        outputType,
        studyProgramViewRepo,
        ctx
      )
    )

  private final class Impl(
      private val printer: ModuleHTMLPrinter,
      private val outputType: PrinterOutputType,
      private val studyProgramViewRepo: StudyProgramViewRepository,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = {
      case Handle(modules) if modules.nonEmpty =>
        studyProgramViewRepo.all().onComplete {
          case Success(sps) =>
            modules.par.foreach {
              case (module, moduleFile) =>
                print(
                  moduleFile.lastModified,
                  module,
                  sp => sps.find(_.id == sp)
                )
            }
          case Failure(t) =>
            logger.error(
              s"""failed to print module
                 |  - cause: unable to fetch study programs from db
                 |  - message: ${t.getMessage}
                 |  - trace: ${t.getStackTrace.mkString(
                  "\n           "
                )}""".stripMargin
            )
        }
    }

    private def print(
        lastModified: LocalDateTime,
        module: Module,
        studyProgram: String => Option[StudyProgramView]
    ): Unit = {
      def go(lang: PrintingLanguage): Unit =
        printer
          .print(
            module,
            lang,
            lastModified,
            outputType,
            studyProgram
          ) match {
          case Left(err) =>
            logError(module, lang, err)
          case Right(output) =>
            logSuccess(module, lang)
            val moduleId    = module.metadata.id
            val moduleTitle = module.metadata.title
            output match {
              case PrinterOutput.Text(content, _, consoleOutput) =>
                logText(moduleTitle, moduleId, content, consoleOutput)
              case PrinterOutput.File(path, consoleOutput) =>
                logFile(moduleTitle, moduleId, path, consoleOutput)
            }
        }
      go(PrintingLanguage.German)
      go(PrintingLanguage.English)
    }

    private def logText(
        title: String,
        id: UUID,
        content: String,
        consoleOutput: String
    ): Unit =
      logger.info(
        s"""successfully converted module to $outputType
           |  - title: $title
           |  - id: $id
           |  - content: ${content.length}
           |  - console output: $consoleOutput""".stripMargin
      )

    private def logFile(
        title: String,
        id: UUID,
        path: String,
        consoleOutput: String
    ): Unit =
      logger.info(
        s"""successfully converted module to $outputType
           |  - title: $title
           |  - id: $id
           |  - path: $path
           |  - console output: $consoleOutput""".stripMargin
      )

    private def logSuccess(
        module: Module,
        language: PrintingLanguage
    ): Unit =
      logger.info(
        s"""successfully printed module in $language
           |  - id: ${module.metadata.id}
           |  - title: ${module.metadata.title}""".stripMargin
      )

    private def logError(
        module: Module,
        language: PrintingLanguage,
        t: Throwable
    ): Unit =
      logger.error(
        s"""failed to print module in $language
           |  - id: ${module.metadata.id}
           |  - message: ${t.getMessage}
           |  - trace: ${t.getStackTrace.mkString(
            "\n           "
          )}""".stripMargin
      )
  }
}
