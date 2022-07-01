package git

import akka.actor.{Actor, ActorLogging, Props}
import controllers.PrinterOutputFormat
import git.ModuleCompendiumPublisher.Go
import parsing.types.ModuleCompendium
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
    with ActorLogging {
  override def receive = { case Go(changes, outputFormat) =>
    changes.added.foreach { mc =>
      log.info(s"printing added module compendium with id ${mc.metadata.id}")
      print(outputFormat, mc)
    }
    changes.modified.foreach { mc =>
      log.info(
        s"printing modified module compendium with id ${mc.metadata.id}"
      )
      print(outputFormat, mc)
    }
    changes.removed.foreach { mc =>
      log.info(s"need to delete module compendium with id ${mc.metadata.id}")
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
            log.debug(
              s"successfully printed module compendium with id ${mc.metadata.id}"
            )
          case PrinterOutput.File(_, _) =>
            log.debug(
              s"successfully printed module compendium with id ${mc.metadata.id}"
            )
        }
      case Left(e) =>
        log.error(
          s"failed to print module compendium with id ${mc.metadata.id}. error: ${e.getMessage}"
        )
    }
  }
}
