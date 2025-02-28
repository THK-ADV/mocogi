package git.subscriber

import java.time.LocalDateTime

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import database.view.ModuleViewRepository
import git.subscriber.ModuleSubscribers.Handle
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.Props
import parsing.types.Module
import play.api.Logging
import service.ModuleCreationService
import service.ModuleService
import service.ModuleUpdatePermissionService

object ModuleDatabaseActor {
  def props(
      moduleService: ModuleService,
      moduleViewRepository: ModuleViewRepository,
      moduleUpdatePermissionService: ModuleUpdatePermissionService,
      moduleCreationService: ModuleCreationService,
      ctx: ExecutionContext
  ) =
    Props(
      new Impl(
        moduleService,
        moduleViewRepository,
        moduleUpdatePermissionService,
        moduleCreationService,
        ctx
      )
    )

  private final class Impl(
      moduleService: ModuleService,
      moduleViewRepository: ModuleViewRepository,
      moduleUpdatePermissionService: ModuleUpdatePermissionService,
      moduleCreationService: ModuleCreationService,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = {
      case Handle(modules) if modules.nonEmpty =>
        val entries = modules.map(m => (m._1, m._2.lastModified))
        update(entries).onComplete {
          case Success((modules, permissions)) =>
            logger.info(
              s"successfully created or updated $modules modules and ${permissions.size} permission entries"
            )
          case Failure(e) =>
            logger.error(
              s"""failed to create or update metadata
                 |  - message: ${e.getMessage}
                 |  - trace: ${e.getStackTrace.mkString(
                  "\n           "
                )}""".stripMargin
            )
        }
    }

    private def update(modules: Seq[(Module, LocalDateTime)]) =
      for {
        created <- moduleService.createOrUpdateMany(modules)
        _       <- moduleViewRepository.refreshView()
        permissions <- moduleUpdatePermissionService.overrideInherited(
          modules.map {
            case (module, _) =>
              (
                module.metadata.id,
                module.metadata.responsibilities.moduleManagement
              )
          }
        )
        _ <- moduleCreationService.deleteMany(modules.map(_._1.metadata.id))
      } yield (created, permissions)
  }
}
