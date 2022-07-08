package providers

import akka.actor.ActorSystem
import git.ModuleCompendiumSubscribers
import git.publisher.ModuleCompendiumPublisher
import parsing.ModuleCompendiumParser

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumPublisherProvider @Inject() (
    system: ActorSystem,
    parser: ModuleCompendiumParser,
    subscribers: ModuleCompendiumSubscribers,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumPublisher] {
  override def get() = ModuleCompendiumPublisher(
    system.actorOf(ModuleCompendiumPublisher.props(parser, subscribers, ctx))
  )
}
