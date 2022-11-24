package git.subscriber

import akka.actor.{Actor, Props}
import controllers.parameter.PrinterOutputFormat
import git.ModuleCompendiumSubscribers.{Added, Modified}
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.{
  ModuleCompendiumGenerationError,
  PrinterOutput,
  PrinterOutputType,
  PrintingLanguage
}
import service.ModuleCompendiumPrintingService

import java.time.LocalDateTime

object ModuleCompendiumPrintingActor {
  def props(
      printingService: ModuleCompendiumPrintingService,
      outputType: PrinterOutputType,
      outputFormat: PrinterOutputFormat
  ) =
    Props(
      new ModuleCompendiumPrintingActor(
        printingService,
        outputType,
        outputFormat
      )
    )
}

private final class ModuleCompendiumPrintingActor(
    private val printingService: ModuleCompendiumPrintingService,
    private val outputType: PrinterOutputType,
    private val outputFormat: PrinterOutputFormat
) extends Actor
    with Logging {

  override def receive = {
    case Added(_, lastModified, _, result) =>
      result.foreach(print(outputFormat, lastModified, _))
    case Modified(_, lastModified, _, result) =>
      result.foreach(print(outputFormat, lastModified, _))
  }

  private def print(
      outputFormat: PrinterOutputFormat,
      lastModified: LocalDateTime,
      mc: ModuleCompendium
  ): Unit = {
    def go(): Either[ModuleCompendiumGenerationError, Unit] =
      printingService
        .print(
          mc,
          lastModified,
          outputType,
          outputFormat,
          PrintingLanguage.German
        )
        .map {
          case PrinterOutput.Text(content, _, consoleOutput) =>
            logText(mc, content, consoleOutput)
          case PrinterOutput.File(path, consoleOutput) =>
            logFile(mc, path, consoleOutput)
        }

    try go().fold(e => logError(mc, e), identity)
    catch { case t: Throwable => logError(mc, t) }
  }

  private def logText(
      mc: ModuleCompendium,
      content: String,
      consoleOutput: String
  ): Unit =
    logger.info(
      s"""successfully printed module compendium
         |  - id: ${mc.metadata.id}
         |  - content: ${content.length}
         |  - console output: $consoleOutput""".stripMargin
    )

  private def logFile(
      mc: ModuleCompendium,
      path: String,
      consoleOutput: String
  ): Unit =
    logger.info(
      s"""successfully printed module compendium
         |  - id: ${mc.metadata.id}
         |  - path: $path
         |  - console output: $consoleOutput""".stripMargin
    )

  private def logError(mc: ModuleCompendium, t: Throwable): Unit =
    logger.error(
      s"""failed to print module compendium
         |  - id: ${mc.metadata.id}
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString("\n           ")}""".stripMargin
    )
}
