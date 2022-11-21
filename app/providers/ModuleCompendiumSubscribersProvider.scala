package providers

import akka.actor.ActorSystem
import git.ModuleCompendiumSubscribers
import git.subscriber.{
  MetadataDatabaseActor,
  ModuleCompendiumLoggingActor,
  ModuleCompendiumPublishActor
}
import parserprinter.ModuleCompendiumParserPrinter
import publisher.KafkaPublisher
import service.MetadataService
import validator.Metadata

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    system: ActorSystem,
    parserPrinter: ModuleCompendiumParserPrinter,
    metadataService: MetadataService,
    publisher: KafkaPublisher[Metadata],
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumSubscribers] {
  override def get(): ModuleCompendiumSubscribers =
    ModuleCompendiumSubscribers(
      List(
        system.actorOf(ModuleCompendiumLoggingActor.props),
        /*        system.actorOf(
          ModuleCompendiumPrintingActor.props(
            parserPrinter,
            PrinterOutputType.HTMLStandaloneFile,
            "output"
          )
        ),*/
        system.actorOf(ModuleCompendiumPublishActor.props(publisher)),
        system.actorOf(MetadataDatabaseActor.props(metadataService, ctx))
      )
    )
}
