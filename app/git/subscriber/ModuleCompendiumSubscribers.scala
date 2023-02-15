package git.subscriber

import akka.actor.ActorRef
import git.GitFilePath
import git.subscriber.ModuleCompendiumSubscribers.{Added, Modified, Removed}
import parsing.types.ModuleCompendium

import java.time.LocalDateTime
import javax.inject.Singleton
import scala.util.Try

object ModuleCompendiumSubscribers {
  case class Added(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  )
  case class Modified(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  )
  case class Removed(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath
  )
}

@Singleton
case class ModuleCompendiumSubscribers(private val value: List[ActorRef]) {
  def added(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  ): Unit =
    value.foreach(_ ! Added(commitId, timestamp, path, result))

  def modified(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  ): Unit =
    value.foreach(_ ! Modified(commitId, timestamp, path, result))

  def removed(
      commitId: String,
      timestamp: LocalDateTime,
      path: GitFilePath
  ): Unit =
    value.foreach(_ ! Removed(commitId, timestamp, path))
}
