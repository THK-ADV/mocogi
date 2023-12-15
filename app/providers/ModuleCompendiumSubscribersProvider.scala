package providers

import akka.actor.ActorSystem
import database.view.ModuleViewRepository
import git.subscriber._
import printing.html.ModuleCompendiumHTMLPrinter
import printing.pandoc.PrinterOutputType
import service.core.StudyProgramService
import service.{ModuleCompendiumService, ModuleUpdatePermissionService}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    printer: ModuleCompendiumHTMLPrinter,
    system: ActorSystem,
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
        system.actorOf(
          ModuleCompendiumPrintingActor.props(
            printer,
            PrinterOutputType.HTMLStandaloneFile(
              configReader.deOutputFolderPath,
              configReader.enOutputFolderPath
            ),
            studyProgramService,
            ctx
          )
        ),
        // system.actorOf(ModuleCompendiumPublishActor.props(publisher)),
        system.actorOf(
          ModuleCompendiumDatabaseActor
            .props(
              metadataService,
              moduleViewRepository,
              moduleUpdatePermissionService,
              ctx
            )
        )
      )
    )
}
