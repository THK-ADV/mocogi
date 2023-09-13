package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import play.api.Logging

object ModuleCompendiumLoggingActor {
  def props = Props(new ModuleCompendiumLoggingActor())
}

final class ModuleCompendiumLoggingActor extends Actor with Logging {
  override def receive = {
    case CreatedOrUpdated(entries) if entries.nonEmpty =>
      logger.info(s"""added module compendium entries
           |  - paths: ${entries.map(_._1.value).mkString("\n")}""".stripMargin)
  }
}
