package providers

import akka.actor.ActorSystem
import git.ModuleCompendiumSubscribers
import git.subscriber.ModuleCompendiumPrintingActor
import printing.{ModuleCompendiumPrinter, PrinterOutputType}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    system: ActorSystem,
    printer: ModuleCompendiumPrinter
) extends Provider[ModuleCompendiumSubscribers] {
  override def get(): ModuleCompendiumSubscribers =
    ModuleCompendiumSubscribers(
      List(
        system.actorOf(
          ModuleCompendiumPrintingActor.props(
            printer,
            PrinterOutputType.HTML
          )
        )
      )
    )
}
