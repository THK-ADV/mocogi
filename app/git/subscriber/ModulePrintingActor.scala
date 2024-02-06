package git.subscriber

import akka.actor.{Actor, Props}
import database.view.StudyProgramViewRepository
import git.subscriber.ModuleSubscribers.CreatedOrUpdated
import models.StudyProgramView
import parsing.types.Module
import play.api.Logging
import printing.PrintingLanguage
import printing.html.ModuleHTMLPrinter
import printing.pandoc.{PrinterOutput, PrinterOutputType}

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

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
      case CreatedOrUpdated(entries) if entries.nonEmpty =>
        studyProgramViewRepo.all() onComplete {
          case Success(sps) =>
            entries.par.foreach { case (_, mc, lastModified) =>
              print(
                lastModified,
                mc,
                sp => sps.find(_.studyProgram.id == sp)
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
        mc: Module,
        studyProgram: String => Option[StudyProgramView]
    ): Unit = {
      def go(lang: PrintingLanguage): Unit =
        printer
          .print(mc, lang, Some(lastModified), outputType, studyProgram) match {
          case Left(err) =>
            logError(mc, lang, err)
          case Right(output) =>
            logSuccess(mc, lang)
            val moduleId = mc.metadata.id
            val moduleTitle = mc.metadata.title
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
        mc: Module,
        language: PrintingLanguage
    ): Unit =
      logger.info(
        s"""successfully printed module in $language
           |  - id: ${mc.metadata.id}
           |  - title: ${mc.metadata.title}""".stripMargin
      )

    private def logError(
        mc: Module,
        language: PrintingLanguage,
        t: Throwable
    ): Unit =
      logger.error(
        s"""failed to print module in $language
           |  - id: ${mc.metadata.id}
           |  - message: ${t.getMessage}
           |  - trace: ${t.getStackTrace.mkString(
            "\n           "
          )}""".stripMargin
      )
  }
}
