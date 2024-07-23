package providers

import akka.actor.ActorSystem
import database.view.{ModuleViewRepository, StudyProgramViewRepository}
import git.subscriber._
import kafka.ModulePublisher
import printing.html.ModuleHTMLPrinter
import printing.pandoc.PrinterOutputType
import service.{ModuleService, ModuleUpdatePermissionService}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleSubscribersProvider @Inject() (
    printer: ModuleHTMLPrinter,
    system: ActorSystem,
    metadataService: ModuleService,
    studyProgramViewRepo: StudyProgramViewRepository,
    moduleViewRepository: ModuleViewRepository,
    moduleUpdatePermissionService: ModuleUpdatePermissionService,
    configReader: ConfigReader,
    ctx: ExecutionContext
) extends Provider[ModuleSubscribers] {
  override def get(): ModuleSubscribers =
    ModuleSubscribers(
      List(
        system.actorOf(
          ModulePrintingActor.props(
            printer,
            PrinterOutputType.HTMLStandaloneFile(
              configReader.deOutputFolderPath,
              configReader.enOutputFolderPath
            ),
            studyProgramViewRepo,
            ctx
          )
        ),
        system.actorOf(
          ModuleDatabaseActor
            .props(
              metadataService,
              moduleViewRepository,
              moduleUpdatePermissionService,
              ctx
            )
        ),
        system.actorOf(
          ModulePublishActor.props(
            new ModulePublisher(
              configReader.kafkaServerUrl,
              configReader.kafkaModuleTopic
            )
          )
        )
      )
    )
}
