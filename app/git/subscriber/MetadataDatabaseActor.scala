package git.subscriber

import akka.actor.{Actor, Props}
import git.GitFilePath
import git.ModuleCompendiumSubscribers.{Added, Modified, Removed}
import play.api.Logging
import service.MetadataService
import validator.Metadata

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MetadataDatabaseActor {
  def props(metadataService: MetadataService, ctx: ExecutionContext) = Props(
    new MetadataDatabaseActor(metadataService, ctx)
  )
}

private final class MetadataDatabaseActor(
    metadataService: MetadataService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case Added(_, path, result) =>
      result.foreach(mc => createOrUpdate(mc.metadata, path))
    case Modified(_, path, result) =>
      result.foreach(mc => createOrUpdate(mc.metadata, path))
    case Removed(_, path) =>
      delete(path)
  }

  private def createOrUpdate(metadata: Metadata, path: GitFilePath): Unit = {
    metadataService.createOrUpdate(metadata, path) onComplete {
      case Success(m) => logSuccess(m, path)
      case Failure(e) => logError(metadata, path, e)
    }
  }

  private def delete(path: GitFilePath): Unit =
    logger.error(
      s"""failed to delete metadata
         |  - git path: ${path.value}
         |  - message: deleting metadata is currently not supported""".stripMargin
    )

  private def logSuccess(
      metadata: Metadata,
      path: GitFilePath
  ): Unit =
    logger.info(
      s"""successfully created or updated metadata
         |  - id: ${metadata.id}
         |  - abbrev: ${metadata.abbrev}
         |  - git path: ${path.value}""".stripMargin
    )

  private def logError(
      metadata: Metadata,
      path: GitFilePath,
      throwable: Throwable
  ): Unit =
    logger.error(
      s"""failed to create or update metadata
         |  - id: ${metadata.id}
         |  - abbrev: ${metadata.abbrev}
         |  - git path: ${path.value}
         |  - message: ${throwable.getMessage}
         |  - trace: ${throwable.getStackTrace.mkString(
          "\n           "
        )}""".stripMargin
    )
}
