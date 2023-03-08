package git.subscriber

import akka.actor.{Actor, Props}
import git.GitFilePath
import git.subscriber.ModuleCompendiumSubscribers.{CreatedOrUpdated, Removed}
import parsing.types.ModuleCompendium
import play.api.Logging

object ModuleCompendiumLoggingActor {
  def props = Props(new ModuleCompendiumLoggingActor())
}

final class ModuleCompendiumLoggingActor() extends Actor with Logging {
  override def receive = {
    case CreatedOrUpdated(commitId, _, path, mc) =>
      log("added", commitId, path, Some(mc))
    case Removed(commitId, _, path) =>
      log("removed", commitId, path, None)
  }

  private def log(
      operation: String,
      commitId: String,
      path: GitFilePath,
      result: Option[ModuleCompendium]
  ): Unit = {
    result match {
      case None =>
        logger.info(s"""$operation module compendium
             |  - commit id: $commitId
             |  - file: ${path.value}""".stripMargin)
      case Some(mc) =>
        logger.info(s"""$operation module compendium
                       |  - commit id: $commitId
                       |  - file: ${path.value}
                       |  - res: ${mc.toString.length}""".stripMargin)
    }
  }
}
