package providers

import akka.actor.ActorSystem
import git.subscriber.ModuleCompendiumMarkdownActor
import printing.pandoc.{PandocApi, PrinterOutputType}

import javax.inject.{Inject, Provider}

final class ModuleCompendiumMarkdownActorProvider @Inject() (
    system: ActorSystem,
    markdownConverter: PandocApi
) extends Provider[ModuleCompendiumMarkdownActor] {
  override def get() = ModuleCompendiumMarkdownActor(
    system.actorOf(
      ModuleCompendiumMarkdownActor.props(
        markdownConverter,
        PrinterOutputType.HTMLStandaloneFile
      )
    )
  )
}
