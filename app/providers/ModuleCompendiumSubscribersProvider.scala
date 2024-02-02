package providers

import akka.actor.ActorSystem
import database.view.{ModuleViewRepository, StudyProgramViewRepository}
import git.subscriber._
import printing.html.ModuleCompendiumHTMLPrinter
import printing.pandoc.PrinterOutputType
import service.{ModuleCompendiumService, ModuleUpdatePermissionService}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleCompendiumSubscribersProvider @Inject() (
    printer: ModuleCompendiumHTMLPrinter,
    system: ActorSystem,
    metadataService: ModuleCompendiumService,
//    publisher: KafkaPublisher[Metadata],
    studyProgramViewRepo: StudyProgramViewRepository,
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
            studyProgramViewRepo,
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
