package git.subscriber

import akka.actor.{Actor, Props}
import controllers.parameter.PrinterOutputFormat
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.{ModuleCompendiumPrinter, PrinterOutput, PrinterOutputType}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object ModuleCompendiumPrintingActor {
  def props(
      printer: ModuleCompendiumPrinter,
      outputType: PrinterOutputType,
      outputFolder: String
  ) =
    Props(
      new ModuleCompendiumPrintingActor(printer, outputType, outputFolder)
    )
}

final class ModuleCompendiumPrintingActor(
    printer: ModuleCompendiumPrinter,
    outputType: PrinterOutputType,
    outputFolder: String
) extends Actor
    with Logging {
  override def receive = { case OnUpdate(changes, outputFormat) =>
    changes.added.foreach { mc =>
      logger.info(s"printing added module compendium with id ${mc.metadata.id}")
      print(outputFormat, mc)
    }
    changes.modified.foreach { mc =>
      logger.info(
        s"printing modified module compendium with id ${mc.metadata.id}"
      )
      print(outputFormat, mc)
    }
    changes.removed.foreach { mc =>
      logger.info(s"need to delete module compendium with id ${mc.metadata.id}")
    }
  }

  private def print(
      outputFormat: PrinterOutputFormat,
      mc: ModuleCompendium
  ): Unit = {
    def go() =
      printer.print(mc, outputType, outputFormat).map {
        case PrinterOutput.Text(content, extension) =>
          val filename = s"${mc.metadata.id}.$extension"
          val newPath = Files.write(
            createPath(filename),
            content.getBytes(StandardCharsets.UTF_8)
          )
          logSuccess(mc, newPath)
        case PrinterOutput.File(file, filename) =>
          val newFile = createPath(filename)
          val newPath = file.moveTo(newFile, replace = true)
          logSuccess(mc, newPath)
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

  private def logSuccess(mc: ModuleCompendium, newPath: Path): Unit =
    logger.info(
      s"""successfully printed module compendium
         |  - id: ${mc.metadata.id}
         |  - path: ${newPath.toString}""".stripMargin
    )
}
