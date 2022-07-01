package git

import akka.actor.ActorRef

case class ModuleCompendiumSubscribers(value: List[ActorRef])
