package git.subscriber

import akka.actor.ActorRef
import git.GitFilePath
import parsing.types.ModuleCompendium

import java.time.LocalDateTime
import javax.inject.Singleton
import ModuleCompendiumSubscribers.{CreatedOrUpdated, Removed}

object ModuleCompendiumSubscribers {
  case class CreatedOrUpdated(
      commitId: String,
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  )
  case class Removed(
      commitId: String,
      timestamp: LocalDateTime,
      entries: Seq[GitFilePath]
  )
}

@Singleton
case class ModuleCompendiumSubscribers(private val value: List[ActorRef]) {
  def createdOrUpdated(
      commitId: String,
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  ): Unit =
    value.foreach(_ ! CreatedOrUpdated(commitId, entries))

  def removed(
      commitId: String,
      timestamp: LocalDateTime,
      entries: Seq[GitFilePath]
  ): Unit =
    value.foreach(_ ! Removed(commitId, timestamp, entries))
}
