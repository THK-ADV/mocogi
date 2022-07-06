/*
package git.subscriber

import akka.actor.{Actor, Props}
import database.{DBInsertionResult, MetadataTableEntry}
import git.GitFilePath
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import parsing.types.Metadata
import play.api.Logging
import service.MetadataService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MetadataDatabaseActor {
  def props(metadataService: MetadataService, ctx: ExecutionContext) = Props(
    new MetadataDatabaseActor(metadataService, ctx)
  )
}

final class MetadataDatabaseActor(
    metadataService: MetadataService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {
  override def receive = { case OnUpdate(changes, _) =>
    changes.added.foreach { case (path, mc) =>
      insertOrUpdate(mc.metadata, path)
    }
    changes.modified.foreach { case (path, mc) =>
      insertOrUpdate(mc.metadata, path)
    }
    changes.removed.foreach { case (_, mc) =>
      logger.info(s"TODO removing metadata with id ${mc.metadata.id}")
    }
  }

  private def insertOrUpdate(metadata: Metadata, gitPath: GitFilePath): Unit =
    metadataService.insertOrUpdate(metadata, gitPath) onComplete {
      case Success(t) => logSuccess(t)
      case Failure(e) => logError(metadata, gitPath, e)
    }

  private def logSuccess(t: (MetadataTableEntry, DBInsertionResult)): Unit = {
    def action(): String = t._2 match {
      case DBInsertionResult.Updated => "updated"
      case DBInsertionResult.Created => "created"
    }
    logger.error(
      s"""successfully ${action()} metadata
         |  - id: ${t._1.id}
         |  - git path: ${t._1.gitPath}
         |  - value: ${t._1.json}""".stripMargin
    )
  }

  private def logError(
      metadata: Metadata,
      gitPath: GitFilePath,
      t: Throwable
  ): Unit = // TODO pull out
    logger.error(
      s"""failed to insert or update metadata
         |  - id: ${metadata.id}
         |  - git path: ${gitPath.value}
         |  - message: ${t.getMessage}
         |  - trace: ${t.getStackTrace.mkString("\n           ")}""".stripMargin
    )
}
*/
