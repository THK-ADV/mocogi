package git.subscriber

import akka.actor.ActorRef
import git.GitFilePath
import git.subscriber.ModuleCompendiumSubscribers.{CreatedOrUpdated, Removed}
import parsing.types.ModuleCompendium

import java.time.LocalDateTime
import javax.inject.Singleton

object ModuleCompendiumSubscribers {
  case class CreatedOrUpdated(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath,
      result: ModuleCompendium
  )
  case class Removed(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath
  )
}

@Singleton
case class ModuleCompendiumSubscribers(private val value: List[ActorRef]) {
  def createdOrUpdated(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath,
      result: ModuleCompendium
  ): Unit =
    value.foreach(_ ! CreatedOrUpdated(commitId, timestamp, path, result))

  def removed(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath
  ): Unit =
    value.foreach(_ ! Removed(commitId, timestamp, path))
}
