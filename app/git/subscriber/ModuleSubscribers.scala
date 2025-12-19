package git.subscriber

import git.subscriber.ModuleSubscribers.Handle
import git.GitFile
import org.apache.pekko.actor.ActorRef
import parsing.types.Module

object ModuleSubscribers {
  case class Handle(modules: Seq[(Module, GitFile.ModuleFile)])
}

case class ModuleSubscribers(private val value: List[ActorRef]) {
  def handle(modules: Seq[(Module, GitFile.ModuleFile)]): Unit =
    value.foreach(_ ! Handle(modules))
}
