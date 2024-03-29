package providers

import akka.actor.ActorSystem
import git.publisher.ModulePublisher
import git.subscriber.ModuleSubscribers
import service.MetadataPipeline

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModulePublisherProvider @Inject() (
    system: ActorSystem,
    pipeline: MetadataPipeline,
    subscribers: ModuleSubscribers,
    ctx: ExecutionContext
) extends Provider[ModulePublisher] {
  override def get() = ModulePublisher(
    system.actorOf(
      ModulePublisher.props(
        pipeline,
        subscribers,
        ctx
      )
    )
  )
}
