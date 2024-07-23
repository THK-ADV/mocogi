package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleSubscribers.CreatedOrUpdated
import kafka.ModulePublisher

object ModulePublishActor {
  def props(publisher: ModulePublisher) = Props(
    new Impl(publisher)
  )

  private final class Impl(
      publisher: ModulePublisher
  ) extends Actor {

    override def receive = {
      case CreatedOrUpdated(modules, _) if modules.nonEmpty =>
        modules.foreach(module => publisher.publish(module))
        publisher.commit()
    }
  }
}
