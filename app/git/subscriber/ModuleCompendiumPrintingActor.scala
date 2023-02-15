package git.subscriber

import akka.actor.{Actor, Props}
import controllers.parameter.PrinterOutputFormat
import ModuleCompendiumSubscribers.{Added, Modified}
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.PrintingLanguage

import java.time.LocalDateTime

object ModuleCompendiumPrintingActor {
  def props(
      markdownActor: ModuleCompendiumMarkdownActor,
      outputFormat: PrinterOutputFormat
  ) =
    Props(
      new ModuleCompendiumPrintingActor(
        markdownActor,
        outputFormat
      )
    )
}

private final class ModuleCompendiumPrintingActor(
    private val markdownActor: ModuleCompendiumMarkdownActor,
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
    val language = PrintingLanguage.German
    outputFormat.printer.printer(language, lastModified).print(mc, "") match {
      case Left(err) =>
        logError(mc, err)
      case Right(print) =>
        logSuccess(mc)
        markdownActor.convert(mc.metadata.title, mc.metadata.id, print)
    }
  }

  private def logSuccess(mc: ModuleCompendium): Unit =
    logger.info(
      s"""successfully printed module compendium
         |  - id: ${mc.metadata.id}
         |  - title: ${mc.metadata.title}""".stripMargin
    )

  private def logError(mc: ModuleCompendium, t: Throwable): Unit =
    logger.error(
      s"""failed to print module compendium
         |  - id: ${mc.metadata.id}
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString("\n           ")}""".stripMargin
    )
}
