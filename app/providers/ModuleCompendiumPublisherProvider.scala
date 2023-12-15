package providers

import akka.actor.ActorSystem
import git.publisher.ModuleCompendiumPublisher
import git.subscriber.ModuleCompendiumSubscribers
import service.MetadataPipeline

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumPublisherProvider @Inject() (
    system: ActorSystem,
    pipeline: MetadataPipeline,
    subscribers: ModuleCompendiumSubscribers,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumPublisher] {
  override def get() = ModuleCompendiumPublisher(
    system.actorOf(
      ModuleCompendiumPublisher.props(
        pipeline,
        subscribers,
        ctx
      )
    )
  )
}
