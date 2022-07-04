package git

import akka.actor.{Actor, Props}
import controllers.PrinterOutputFormat
import git.ModuleCompendiumPublisher.OnUpdate
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.{ModuleCompendiumPrinter, PrinterOutput, PrinterOutputType}

object ModuleCompendiumPrintingActor {
  def props(printer: ModuleCompendiumPrinter, outputType: PrinterOutputType) =
    Props(
      new ModuleCompendiumPrintingActor(printer, outputType)
    )
}

final class ModuleCompendiumPrintingActor(
    printer: ModuleCompendiumPrinter,
    outputType: PrinterOutputType
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
    printer.print(mc, outputType, outputFormat) match {
      case Right(output) =>
        output match {
          case PrinterOutput.Text(_) =>
            logger.info(
              s"successfully printed module compendium with id ${mc.metadata.id}"
            )
          case PrinterOutput.File(_, _) =>
            logger.info(
              s"successfully printed module compendium with id ${mc.metadata.id}"
            )
        }
      case Left(e) =>
        logger.error(
          s"failed to print module compendium with id ${mc.metadata.id}. error: ${e.getMessage}"
        )
    }
  }
}
