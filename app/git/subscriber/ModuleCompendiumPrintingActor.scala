package git.subscriber

import akka.actor.{Actor, Props}
import controllers.parameter.PrinterOutputFormat
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.PrintingLanguage
import service.core.{StudyProgramService, StudyProgramShort}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumPrintingActor {
  def props(
      markdownActor: ModuleCompendiumMarkdownActor,
      outputFormat: PrinterOutputFormat,
      studyProgramService: StudyProgramService,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumPrintingActor(
        markdownActor,
        outputFormat,
        studyProgramService,
        ctx
      )
    )
}

private final class ModuleCompendiumPrintingActor(
    private val markdownActor: ModuleCompendiumMarkdownActor,
    private val outputFormat: PrinterOutputFormat,
    private val studyProgramService: StudyProgramService,
    private implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = { case CreatedOrUpdated(_, entries) =>
    studyProgramService.allShort() onComplete {
      case Success(sps) =>
        entries.foreach { case (_, mc, lastModified) =>
          print(
            outputFormat,
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
      outputFormat: PrinterOutputFormat,
      lastModified: LocalDateTime,
      mc: ModuleCompendium,
      studyProgram: String => Option[StudyProgramShort]
  ): Unit = {
    val language = PrintingLanguage.German
    outputFormat.printer
      .printer(studyProgram)(language, lastModified)
      .print(mc, "") match {
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
