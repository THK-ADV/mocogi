package git.subscriber

import akka.actor.{Actor, Props}
import git.GitFilePath
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import parsing.types.ModuleCompendium
import play.api.Logging
import service.{ModuleCompendiumService, ModuleDraftService}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumDatabaseActor {
  def props(
      metadataService: ModuleCompendiumService,
      moduleDraftService: ModuleDraftService,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumDatabaseActor(
        metadataService,
        moduleDraftService,
        ctx
      )
    )
}

private final class ModuleCompendiumDatabaseActor(
    metadataService: ModuleCompendiumService,
    moduleDraftService: ModuleDraftService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case CreatedOrUpdated(entries) if entries.nonEmpty =>
      createOrUpdate(entries)
  }

  private def createOrUpdate(
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  ): Unit = {
    val res = for {
      mcs <- metadataService.createOrUpdateMany(entries)
      _ <- moduleDraftService.deleteDrafts(mcs.map(_.metadata.id))
    } yield mcs

    res.onComplete {
      case Success(mcs) =>
        logSuccess(mcs)
      case Failure(e) =>
        logError(e)
    }
  }

  private def logSuccess(mcs: Seq[ModuleCompendium]): Unit =
    logger.info(
      s"""successfully created or updated metadata entries
         |  - entries: ${mcs
          .map(a => (a.metadata.id, a.metadata.abbrev))
          .mkString("\n    ")}""".stripMargin
    )

  private def logError(throwable: Throwable): Unit =
    logger.error(
      s"""failed to create or update metadata
         |  - message: ${throwable.getMessage}
         |  - trace: ${throwable.getStackTrace.mkString(
          "\n           "
        )}""".stripMargin
    )
}
