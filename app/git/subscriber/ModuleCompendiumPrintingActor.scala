package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import models.StudyProgramShort
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.PrintingLanguage
import printing.html.ModuleCompendiumHTMLPrinter
import printing.pandoc.{PrinterOutput, PrinterOutputType}
import service.core.StudyProgramService

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumPrintingActor {
  def props(
      printer: ModuleCompendiumHTMLPrinter,
      outputType: PrinterOutputType,
      studyProgramService: StudyProgramService,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumPrintingActor(
        printer,
        outputType,
        studyProgramService,
        ctx
      )
    )
}

private final class ModuleCompendiumPrintingActor(
    private val printer: ModuleCompendiumHTMLPrinter,
    private val outputType: PrinterOutputType,
    private val studyProgramService: StudyProgramService,
    private implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case CreatedOrUpdated(entries) if entries.nonEmpty =>
      studyProgramService.allShort() onComplete {
        case Success(sps) =>
          entries.par.foreach { case (_, mc, lastModified) =>
            print(
              lastModified,
              mc,
              sp => sps.find(_.id == sp)
            )
          }
        case Failure(t) =>
          logger.error(
            s"""failed to print module compendium
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
      mc: ModuleCompendium,
      studyProgram: String => Option[StudyProgramShort]
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
      s"""successfully converted module compendium to $outputType
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
      s"""successfully converted module compendium to $outputType
         |  - title: $title
         |  - id: $id
         |  - path: $path
         |  - console output: $consoleOutput""".stripMargin
    )

  private def logSuccess(
      mc: ModuleCompendium,
      language: PrintingLanguage
  ): Unit =
    logger.info(
      s"""successfully printed module compendium in $language
         |  - id: ${mc.metadata.id}
         |  - title: ${mc.metadata.title}""".stripMargin
    )

  private def logError(
      mc: ModuleCompendium,
      language: PrintingLanguage,
      t: Throwable
  ): Unit =
    logger.error(
      s"""failed to print module compendium in $language
         |  - id: ${mc.metadata.id}
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString("\n           ")}""".stripMargin
    )
}
