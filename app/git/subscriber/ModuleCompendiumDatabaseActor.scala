package git.subscriber

import akka.actor.{Actor, Props}
import database.view.ModuleViewRepository
import git.GitFilePath
import git.subscriber.ModuleCompendiumSubscribers.{CreatedOrUpdated, Removed}
import parsing.types.ModuleCompendium
import play.api.Logging
import service.ModuleCompendiumService

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumDatabaseActor {
  def props(
      metadataService: ModuleCompendiumService,
      moduleViewRepository: ModuleViewRepository,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumDatabaseActor(
        metadataService,
        moduleViewRepository,
        ctx
      )
    )
}

private final class ModuleCompendiumDatabaseActor(
    metadataService: ModuleCompendiumService,
    moduleViewRepository: ModuleViewRepository,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case CreatedOrUpdated(_, entries) =>
      createOrUpdate(entries)
    case Removed(_, _, entries) =>
      delete(entries)
  }

  private def createOrUpdate(
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  ): Unit =
    metadataService
      .createOrUpdateMany(entries)
      .flatMap(xs => moduleViewRepository.refreshView().map(_ => xs))
      .onComplete {
        case Success(mcs) =>
          logSuccess(mcs)
        case Failure(e) =>
          logError(e)
      }

  private def logSuccess(mcs: Seq[ModuleCompendium]): Unit =
    logger.info(
      s"""successfully created or updated metadata entries
         |  - entries: ${mcs
          .map(a => (a.metadata.id, a.metadata.abbrev))
          .mkString("\n    ")}""".stripMargin
    )

  private def delete(entries: Seq[GitFilePath]): Unit =
    logger.info(
      s"""failed to delete metadata
         |  - git path: ${entries.map(_.value).mkString(", ")}
         |  - message: deleting metadata is currently not supported""".stripMargin
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
