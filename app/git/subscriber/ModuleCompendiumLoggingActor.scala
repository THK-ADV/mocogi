package git.subscriber

import akka.actor.{Actor, Props}
import git.GitFilePath
import git.ModuleCompendiumSubscribers.{Added, Modified, Removed}
import parsing.types.ModuleCompendium
import play.api.Logging

import scala.util.{Failure, Success, Try}

object ModuleCompendiumLoggingActor {
  def props = Props(new ModuleCompendiumLoggingActor())
}

final class ModuleCompendiumLoggingActor() extends Actor with Logging {
  override def receive = {
    case Added(commitId, _, path, result) =>
      log("added", commitId, path, Some(result))
    case Modified(commitId, _, path, result) =>
      log("modified", commitId, path, Some(result))
    case Removed(commitId, _, path) =>
      log("removed", commitId, path, None)
  }

  private def log(
      operation: String,
      commitId: String,
      path: GitFilePath,
      result: Option[Try[ModuleCompendium]]
  ): Unit = {
    result match {
      case None =>
        logger.info(s"""$operation module compendium
             |  - commit id: $commitId
             |  - file: ${path.value}""".stripMargin)
      case Some(Success(mc)) =>
        logger.info(s"""$operation module compendium
                       |  - commit id: $commitId
                       |  - file: ${path.value}
                       |  - res: ${mc.toString.length}""".stripMargin)
      case Some(Failure(e)) =>
        logger.error(s"""failed to parse and validate
                        |  - commit id: $commitId
                        |  - file: ${path.value}
                        |  - message: ${e.getMessage}
                        |  - trace: ${e.getStackTrace.mkString(
                         "\n           "
                       )}""".stripMargin)
    }
  }
}
