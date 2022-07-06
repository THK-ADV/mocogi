package providers

import akka.actor.ActorSystem
import controllers.ModuleCompendiumParsingController
import git.ModuleCompendiumSubscribers
import git.subscriber.{
  ModuleCompendiumJsonStreamActor,
  ModuleCompendiumPrintingActor
}
import parserprinter.ModuleCompendiumParserPrinter
import printing.PrinterOutputType

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    system: ActorSystem,
    parserPrinter: ModuleCompendiumParserPrinter
) extends Provider[ModuleCompendiumSubscribers] {
  override def get(): ModuleCompendiumSubscribers =
    ModuleCompendiumSubscribers(
      List(
        system.actorOf(
          ModuleCompendiumPrintingActor.props(
            parserPrinter,
            PrinterOutputType.HTMLStandalone,
            "output"
          )
        ),
        system.actorOf(
          ModuleCompendiumJsonStreamActor.props(
            ModuleCompendiumParsingController.moduleCompendiumFormat.writes
          )
        )
      )
    )
}
