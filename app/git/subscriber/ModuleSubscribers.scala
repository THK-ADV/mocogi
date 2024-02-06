package git.subscriber

import akka.actor.ActorRef
import git.GitFilePath
import git.subscriber.ModuleSubscribers.CreatedOrUpdated
import parsing.types.Module

import java.time.LocalDateTime
import javax.inject.Singleton

object ModuleSubscribers {
  case class CreatedOrUpdated(
      entries: Seq[(GitFilePath, Module, LocalDateTime)]
  )
}

@Singleton
case class ModuleSubscribers(private val value: List[ActorRef]) {
  def createdOrUpdated(
      entries: Seq[(GitFilePath, Module, LocalDateTime)]
  ): Unit = value.foreach(_ ! CreatedOrUpdated(entries))
}
