package providers

import akka.actor.ActorSystem
import database.view.ModuleViewRepository
import git.subscriber._
import printing.markdown.ModuleCompendiumPrinter
import service.core.StudyProgramService
import service.{ModuleCompendiumService, ModuleUpdatePermissionService}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    printer: ModuleCompendiumPrinter,
    system: ActorSystem,
    moduleCompendiumMarkdownActor: ModuleCompendiumMarkdownActor,
    metadataService: ModuleCompendiumService,
//    publisher: KafkaPublisher[Metadata],
    studyProgramService: StudyProgramService,
    moduleViewRepository: ModuleViewRepository,
    moduleUpdatePermissionService: ModuleUpdatePermissionService,
    configReader: ConfigReader,
    ctx: ExecutionContext
) extends Provider[ModuleCompendiumSubscribers] {
  // TODO maybe this should be one actor which does all the work transactional and
  //  recover somehow if there is a failure
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
        // system.actorOf(ModuleCompendiumPublishActor.props(publisher)),
        system.actorOf(
          ModuleCompendiumDatabaseActor
            .props(metadataService, ctx)
        ),
        system.actorOf(
          ModuleCompendiumViewUpdateActor.props(moduleViewRepository, ctx)
        ),
        system.actorOf(
          ModuleCompendiumPermissionUpdateActor.props(
            moduleUpdatePermissionService,
            ctx
          )
        )
      )
    )
}
