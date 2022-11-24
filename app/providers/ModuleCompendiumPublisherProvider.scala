package providers

import akka.actor.ActorSystem
import git.ModuleCompendiumSubscribers
import git.publisher.ModuleCompendiumPublisher
import service.ModuleCompendiumParsingValidator

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumPublisherProvider @Inject() (
    system: ActorSystem,
    parsingValidator: ModuleCompendiumParsingValidator,
    subscribers: ModuleCompendiumSubscribers,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumPublisher] {
  override def get() = ModuleCompendiumPublisher(
    system.actorOf(
      ModuleCompendiumPublisher.props(parsingValidator, subscribers, ctx)
    )
  )
}
