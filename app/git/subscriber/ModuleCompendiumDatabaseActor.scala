package git.subscriber

import akka.actor.{Actor, Props}
import git.GitFilePath
import ModuleCompendiumSubscribers.{Added, Modified, Removed}
import parsing.types.ModuleCompendium
import play.api.Logging
import service.ModuleCompendiumService

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumDatabaseActor {
  def props(metadataService: ModuleCompendiumService, ctx: ExecutionContext) =
    Props(
      new ModuleCompendiumDatabaseActor(metadataService, ctx)
    )
}

private final class ModuleCompendiumDatabaseActor(
    metadataService: ModuleCompendiumService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case Added(_, timestamp, path, result) =>
      result.foreach(mc => createOrUpdate(mc, path, timestamp))
    case Modified(_, timestamp, path, result) =>
      result.foreach(mc => createOrUpdate(mc, path, timestamp))
    case Removed(_, _, path) =>
      delete(path)
  }

  private def createOrUpdate(
      moduleCompendium: ModuleCompendium,
      path: GitFilePath,
      timestamp: LocalDateTime
  ): Unit =
    metadataService.createOrUpdate(
      moduleCompendium,
      path,
      timestamp
    ) onComplete {
      case Success(m) => logSuccess(m, path)
      case Failure(e) => logError(moduleCompendium, path, e)
    }

  private def delete(path: GitFilePath): Unit =
    logger.error(
      s"""failed to delete metadata
         |  - git path: ${path.value}
         |  - message: deleting metadata is currently not supported""".stripMargin
    )

  private def logSuccess(
      moduleCompendium: ModuleCompendium,
      path: GitFilePath
  ): Unit =
    logger.info(
      s"""successfully created or updated metadata
         |  - id: ${moduleCompendium.metadata.id}
         |  - abbrev: ${moduleCompendium.metadata.abbrev}
         |  - git path: ${path.value}""".stripMargin
    )

  private def logError(
      moduleCompendium: ModuleCompendium,
      path: GitFilePath,
      throwable: Throwable
  ): Unit =
    logger.error(
      s"""failed to create or update metadata
         |  - id: ${moduleCompendium.metadata.id}
         |  - abbrev: ${moduleCompendium.metadata.abbrev}
         |  - git path: ${path.value}
         |  - message: ${throwable.getMessage}
         |  - trace: ${throwable.getStackTrace.mkString(
          "\n           "
        )}""".stripMargin
    )
}
