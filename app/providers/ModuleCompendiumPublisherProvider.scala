package providers

import akka.actor.ActorSystem
import git.publisher.ModuleCompendiumPublisher
import git.subscriber.ModuleCompendiumSubscribers
import service.{MetadataParsingService, ModuleCompendiumService}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumPublisherProvider @Inject() (
    system: ActorSystem,
    metadataParsingService: MetadataParsingService,
    moduleCompendiumService: ModuleCompendiumService,
    subscribers: ModuleCompendiumSubscribers,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumPublisher] {
  override def get() = ModuleCompendiumPublisher(
    system.actorOf(
      ModuleCompendiumPublisher.props(
        metadataParsingService,
        moduleCompendiumService,
        subscribers,
        ctx
      )
    )
  )
}
