package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleCompendiumSubscribers.{CreatedOrUpdated, Removed}
import play.api.Logging

object ModuleCompendiumLoggingActor {
  def props = Props(new ModuleCompendiumLoggingActor())
}

final class ModuleCompendiumLoggingActor() extends Actor with Logging {
  override def receive = {
    case CreatedOrUpdated(commitId, entries) if entries.nonEmpty =>
      logger.info(s"""added module compendium entries
           |  - commit id: $commitId
           |  - paths: ${entries.map(_._1.value).mkString("\n")}""".stripMargin)
    case Removed(commitId, _, paths) if paths.nonEmpty =>
      logger.info(s"""removed module compendium entries
           |  - commit id: $commitId
           |  - paths: ${paths.map(_.value).mkString("\n")}""".stripMargin)
  }
}
