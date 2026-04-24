package git.subscriber

import git.subscriber.ModuleSubscribers.Handle
import git.GitFile
import logging.CorrelationId
import org.apache.pekko.actor.ActorRef
import parsing.types.Module

object ModuleSubscribers {
  case class Handle(modules: Seq[(Module, GitFile.ModuleFile)], correlationId: CorrelationId)
}

case class ModuleSubscribers(private val value: List[ActorRef]) {
  def handle(modules: Seq[(Module, GitFile.ModuleFile)], correlationId: CorrelationId): Unit =
    value.foreach(_ ! Handle(modules, correlationId))
}
