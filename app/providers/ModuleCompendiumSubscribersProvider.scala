package providers

import akka.actor.ActorSystem
import controllers.parameter.PrinterOutputFormat
import git.ModuleCompendiumSubscribers
import git.subscriber.{
  ModuleCompendiumDatabaseActor,
  ModuleCompendiumLoggingActor,
  ModuleCompendiumPrintingActor,
  ModuleCompendiumPublishActor
}
import printing.PrinterOutputType
import publisher.KafkaPublisher
import service.{ModuleCompendiumService, ModuleCompendiumPrintingService}
import validator.Metadata

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    system: ActorSystem,
    parserPrinter: ModuleCompendiumPrintingService,
    metadataService: ModuleCompendiumService,
    publisher: KafkaPublisher[Metadata],
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumSubscribers] {
  override def get(): ModuleCompendiumSubscribers =
    ModuleCompendiumSubscribers(
      List(
        system.actorOf(ModuleCompendiumLoggingActor.props),
        system.actorOf(
          ModuleCompendiumPrintingActor.props(
            parserPrinter,
            PrinterOutputType.HTMLStandaloneFile,
            PrinterOutputFormat.DefaultPrinter
          )
        ),
        system.actorOf(ModuleCompendiumPublishActor.props(publisher)),
        system.actorOf(ModuleCompendiumDatabaseActor.props(metadataService, ctx))
      )
    )
}
