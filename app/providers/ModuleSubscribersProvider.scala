package providers

import database.view.{ModuleViewRepository, StudyProgramViewRepository}
import git.subscriber._
import kafka.Topics
import ops.ConfigurationOps.Ops
import org.apache.pekko.actor.ActorSystem
import play.api.Configuration
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
    config: Configuration,
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
            kafkaServerUrl,
            ctx,
            Topics(
              kafkaModuleCreatedTopic,
              kafkaModuleUpdatedTopic,
              kafkaModuleDeletedTopic
            )
          )
        )
      )
    )

  private def kafkaServerUrl: String =
    config.nonEmptyString("kafka.serverUrl")

  private def kafkaModuleCreatedTopic: String =
    config.nonEmptyString("kafka.topic.module.created")

  private def kafkaModuleUpdatedTopic: String =
    config.nonEmptyString("kafka.topic.module.updated")

  private def kafkaModuleDeletedTopic: String =
    config.nonEmptyString("kafka.topic.module.deleted")
}
