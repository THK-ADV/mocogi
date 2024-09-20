package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import git.publisher.ModulePublisher
import git.subscriber.ModuleSubscribers
import org.apache.pekko.actor.ActorSystem
import service.MetadataPipeline

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
