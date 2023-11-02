package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import parsing.types.ModuleCompendium
import play.api.Logging
import service.ModuleCompendiumService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumDatabaseActor {
  def props(
      metadataService: ModuleCompendiumService,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumDatabaseActor(
        metadataService,
        ctx
      )
    )
}

private final class ModuleCompendiumDatabaseActor(
    metadataService: ModuleCompendiumService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case CreatedOrUpdated(entries) if entries.nonEmpty =>
      metadataService.createOrUpdateMany(entries).onComplete {
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
