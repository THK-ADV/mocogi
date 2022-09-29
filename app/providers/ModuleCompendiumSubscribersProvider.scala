package providers

import akka.actor.ActorSystem
import git.ModuleCompendiumSubscribers
import git.subscriber.{MetadataDatabaseActor, ModuleCompendiumPrintingActor, ModuleCompendiumPublishActor}
import parserprinter.ModuleCompendiumParserPrinter
import parsing.types.Metadata
import printing.PrinterOutputType
import publisher.KafkaPublisher
import service.MetadataService
import validator.ValidMetadata

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    system: ActorSystem,
    parserPrinter: ModuleCompendiumParserPrinter,
    metadataService: MetadataService,
    publisher: KafkaPublisher[ValidMetadata],
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumSubscribers] {
  override def get(): ModuleCompendiumSubscribers =
    ModuleCompendiumSubscribers(
      List(
        system.actorOf(
          ModuleCompendiumPrintingActor.props(
            parserPrinter,
            PrinterOutputType.HTMLStandaloneFile,
            "output"
          )
        ),
        system.actorOf(ModuleCompendiumPublishActor.props(publisher)),
        system.actorOf(MetadataDatabaseActor.props(metadataService, ctx))
      )
    )
}
