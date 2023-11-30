package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import models.StudyProgramShort
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.PrintingLanguage
import printing.markdown.ModuleCompendiumMarkdownPrinter
import printing.pandoc.{PandocApi, PrinterOutput, PrinterOutputType}
import service.core.StudyProgramService

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumPrintingActor {
  def props(
      printer: ModuleCompendiumMarkdownPrinter,
      pandocApi: PandocApi,
      outputType: PrinterOutputType,
      studyProgramService: StudyProgramService,
      deOutputFolderPath: String,
      enOutputFolderPath: String,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumPrintingActor(
        printer,
        pandocApi,
        outputType,
        studyProgramService,
        deOutputFolderPath,
        enOutputFolderPath,
        ctx
      )
    )
}

private final class ModuleCompendiumPrintingActor(
    private val printer: ModuleCompendiumMarkdownPrinter,
    private val pandocApi: PandocApi,
    private val outputType: PrinterOutputType,
    private val studyProgramService: StudyProgramService,
    private val deOutputFolderPath: String,
    private val enOutputFolderPath: String,
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
              sp => sps.find(_.abbrev == sp)
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
    def go(lang: PrintingLanguage, path: String): Unit =
      printer
        .print(studyProgram, lang, lastModified, mc) match {
        case Left(err) =>
          logError(mc, lang, path, err)
        case Right(print) =>
          logSuccess(mc, lang, path)
          val moduleId = mc.metadata.id
          val moduleTitle = mc.metadata.title
          pandocApi.run(moduleId, outputType, print, path) match {
            case Left(err) =>
              logErrorConvert(moduleTitle, moduleId, err)
            case Right(output) =>
              output match {
                case PrinterOutput.Text(content, _, consoleOutput) =>
                  logText(moduleTitle, moduleId, content, consoleOutput)
                case PrinterOutput.File(path, consoleOutput) =>
                  logFile(moduleTitle, moduleId, path, consoleOutput)
              }
          }
      }
    go(PrintingLanguage.German, deOutputFolderPath)
    go(PrintingLanguage.English, enOutputFolderPath)
  }

  private def logErrorConvert(title: String, id: UUID, t: Throwable): Unit =
    logger.error(
      s"""failed to convert module compendium to $outputType
         |  - title: $title
         |  - id: $id
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString(
          "\n           "
        )}""".stripMargin
    )

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
      language: PrintingLanguage,
      path: String
  ): Unit =
    logger.info(
      s"""successfully printed module compendium in $language to $path
         |  - id: ${mc.metadata.id}
         |  - title: ${mc.metadata.title}""".stripMargin
    )

  private def logError(
      mc: ModuleCompendium,
      language: PrintingLanguage,
      path: String,
      t: Throwable
  ): Unit =
    logger.error(
      s"""failed to print module compendium in $language to $path
         |  - id: ${mc.metadata.id}
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString("\n           ")}""".stripMargin
    )
}
