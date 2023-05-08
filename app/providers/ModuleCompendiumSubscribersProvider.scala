package providers

import akka.actor.ActorSystem
import git.subscriber._
import printing.markdown.ModuleCompendiumPrinter
import publisher.KafkaPublisher
import service.ModuleCompendiumService
import service.core.StudyProgramService
import validator.Metadata

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    printer: ModuleCompendiumPrinter,
    system: ActorSystem,
    moduleCompendiumMarkdownActor: ModuleCompendiumMarkdownActor,
    metadataService: ModuleCompendiumService,
    publisher: KafkaPublisher[Metadata],
    studyProgramService: StudyProgramService,
    configReader: ConfigReader,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumSubscribers] {
  override def get(): ModuleCompendiumSubscribers =
    ModuleCompendiumSubscribers(
      List(
        system.actorOf(ModuleCompendiumLoggingActor.props),
        system.actorOf(
          ModuleCompendiumPrintingActor.props(
            printer,
            moduleCompendiumMarkdownActor,
            studyProgramService,
            configReader.deOutputFolderPath,
            configReader.enOutputFolderPath,
            ctx
          )
        ),
        system.actorOf(ModuleCompendiumPublishActor.props(publisher)),
        system.actorOf(
          ModuleCompendiumDatabaseActor.props(metadataService, ctx)
        )
      )
    )
}
