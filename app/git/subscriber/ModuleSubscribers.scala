package git.subscriber

import akka.actor.ActorRef
import git.subscriber.ModuleSubscribers.CreatedOrUpdated
import parsing.types.Module

import java.time.LocalDateTime
import javax.inject.Singleton

object ModuleSubscribers {
  case class CreatedOrUpdated(modules: Seq[Module], lastModified: LocalDateTime)
}

@Singleton
case class ModuleSubscribers(private val value: List[ActorRef]) {
  def createdOrUpdated(
      modules: Seq[Module],
      lastModified: LocalDateTime
  ): Unit = value.foreach(_ ! CreatedOrUpdated(modules, lastModified))
}
