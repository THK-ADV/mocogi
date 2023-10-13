package git.subscriber

import akka.actor.ActorRef
import git.GitFilePath
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import parsing.types.ModuleCompendium

import java.time.LocalDateTime
import javax.inject.Singleton

object ModuleCompendiumSubscribers {
  case class CreatedOrUpdated(
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  )
}

@Singleton
case class ModuleCompendiumSubscribers(private val value: List[ActorRef]) {
  def createdOrUpdated(
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  ): Unit = value.foreach(_ ! CreatedOrUpdated(entries))
}
