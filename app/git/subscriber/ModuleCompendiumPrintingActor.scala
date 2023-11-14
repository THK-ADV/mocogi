package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import parsing.types.ModuleCompendium
import play.api.Logging
import printing.PrintingLanguage
import printing.markdown.ModuleCompendiumMarkdownPrinter
import service.core.{StudyProgramService, StudyProgramShort}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumPrintingActor {
  def props(
      printer: ModuleCompendiumMarkdownPrinter,
      markdownActor: ModuleCompendiumMarkdownActor,
      studyProgramService: StudyProgramService,
      deOutputFolderPath: String,
      enOutputFolderPath: String,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumPrintingActor(
        printer,
        markdownActor,
        studyProgramService,
        deOutputFolderPath,
        enOutputFolderPath,
        ctx
      )
    )
}

private final class ModuleCompendiumPrintingActor(
    private val printer: ModuleCompendiumMarkdownPrinter,
    private val markdownActor: ModuleCompendiumMarkdownActor,
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
          entries.foreach { case (_, mc, lastModified) =>
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
        .printer(studyProgram)(lang, lastModified)
        .print(mc, "") match {
        case Left(err) =>
          logError(mc, lang, path, err)
        case Right(print) =>
          logSuccess(mc, lang, path)
          markdownActor.convert(mc.metadata.title, mc.metadata.id, print, path)
      }
    go(PrintingLanguage.German, deOutputFolderPath)
    go(PrintingLanguage.English, enOutputFolderPath)
  }

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
