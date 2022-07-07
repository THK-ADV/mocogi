package providers

import akka.actor.ActorSystem
import controllers.ModuleCompendiumParsingController
import git.ModuleCompendiumSubscribers
import git.subscriber.{
  MetadataDatabaseActor,
  ModuleCompendiumJsonStreamActor,
  ModuleCompendiumPrintingActor
}
import parserprinter.ModuleCompendiumParserPrinter
import printing.PrinterOutputType
import service.MetadataService

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    system: ActorSystem,
    parserPrinter: ModuleCompendiumParserPrinter,
    metadataService: MetadataService,
    ctx: ExecutionContext
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
        ),
        system.actorOf(
          MetadataDatabaseActor.props(
            metadataService,
            ctx
          )
        )
      )
    )
}
