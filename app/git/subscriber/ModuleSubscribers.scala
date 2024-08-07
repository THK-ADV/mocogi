package git.subscriber

import akka.actor.ActorRef
import git.GitFile
import git.subscriber.ModuleSubscribers.Handle
import parsing.types.Module

import java.time.LocalDateTime
import javax.inject.Singleton

object ModuleSubscribers {
  case class Handle(
      modules: Seq[(Module, GitFile.ModuleFile)],
      lastModified: LocalDateTime
  )
}

@Singleton
case class ModuleSubscribers(private val value: List[ActorRef]) {
  def handle(
      modules: Seq[(Module, GitFile.ModuleFile)],
      lastModified: LocalDateTime
  ): Unit =
    value.foreach(_ ! Handle(modules, lastModified))
}
