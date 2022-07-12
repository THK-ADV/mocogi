package git.subscriber

import akka.actor.{Actor, Props}
import controllers.parameter.PrinterOutputFormat
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import parserprinter.ModuleCompendiumParserPrinter
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.{
  ModuleCompendiumGenerationError,
  PrinterOutput,
  PrinterOutputType
}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object ModuleCompendiumPrintingActor {
  def props(
      parserPrinter: ModuleCompendiumParserPrinter,
      outputType: PrinterOutputType,
      outputFolder: String
  ) =
    Props(
      new ModuleCompendiumPrintingActor(parserPrinter, outputType, outputFolder)
    )
}

private final class ModuleCompendiumPrintingActor(
    private val parserPrinter: ModuleCompendiumParserPrinter,
    private val outputType: PrinterOutputType,
    private val outputFolder: String
) extends Actor
    with Logging {

  override def receive = { case OnUpdate(changes, outputFormat) =>
    changes.added.foreach { case (_, mc) =>
      print(outputFormat, mc)
    }
    changes.modified.foreach { case (_, mc) =>
      print(outputFormat, mc)
    }
    changes.removed.foreach { path =>
      logger.info(s"need to delete module compendium with path ${path.value}")
    }
  }

  private def print(
      outputFormat: PrinterOutputFormat,
      mc: ModuleCompendium
  ): Unit = {
    def go(): Either[ModuleCompendiumGenerationError, Unit] =
      parserPrinter.print(mc, outputType, outputFormat).map {
        case PrinterOutput.Text(content, extension) =>
          val filename = s"${mc.metadata.id}.$extension"
          val newPath = Files.write(
            createPath(filename),
            content.getBytes(StandardCharsets.UTF_8)
          )
          logSuccess(mc, newPath.toString)
        case PrinterOutput.File(path) =>
          logSuccess(mc, path)
      }

    try go().fold(e => logError(mc, e), identity)
    catch { case t: Throwable => logError(mc, t) }
  }

  private def createPath(filename: String) =
    Paths.get(s"$outputFolder/$filename")

  private def logError(mc: ModuleCompendium, t: Throwable): Unit =
    logger.error(
      s"""failed to print module compendium
         |  - id: ${mc.metadata.id}
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString("\n           ")}""".stripMargin
    )

  private def logSuccess(mc: ModuleCompendium, path: String): Unit =
    logger.info(
      s"""successfully printed module compendium
         |  - id: ${mc.metadata.id}
         |  - path: ${path}""".stripMargin
    )
}
