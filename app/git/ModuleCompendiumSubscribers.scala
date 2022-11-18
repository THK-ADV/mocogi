package git

import akka.actor.ActorRef
import git.ModuleCompendiumSubscribers.{Added, Modified, Removed}
import parsing.types.ModuleCompendium

import javax.inject.Singleton
import scala.util.Try

object ModuleCompendiumSubscribers {
  case class Added(
      commitId: String,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  )
  case class Modified(
      commitId: String,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  )
  case class Removed(
      commitId: String,
      path: GitFilePath
  )
}

@Singleton
case class ModuleCompendiumSubscribers(private val value: List[ActorRef]) {
  def added(
      commitId: String,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  ): Unit =
    value.foreach(_ ! Added(commitId, path, result))

  def modified(
      commitId: String,
      path: GitFilePath,
      result: Try[ModuleCompendium]
  ): Unit =
    value.foreach(_ ! Modified(commitId, path, result))

  def removed(
      commitId: String,
      path: GitFilePath
  ): Unit =
    value.foreach(_ ! Removed(commitId, path))
}
