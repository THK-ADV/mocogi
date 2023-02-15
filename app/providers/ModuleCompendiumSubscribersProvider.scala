package providers

import akka.actor.ActorSystem
import controllers.parameter.PrinterOutputFormat
import git.subscriber._
import publisher.KafkaPublisher
import service.ModuleCompendiumService
import validator.Metadata

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    system: ActorSystem,
    moduleCompendiumMarkdownActor: ModuleCompendiumMarkdownActor,
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
            moduleCompendiumMarkdownActor,
            PrinterOutputFormat.DefaultPrinter
          )
        ),
        system.actorOf(ModuleCompendiumPublishActor.props(publisher)),
        system.actorOf(
          ModuleCompendiumDatabaseActor.props(metadataService, ctx)
        )
      )
    )
}
