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
import service.CreateNewModuleService
import service.ModuleService
import service.ModuleUpdatePermissionService

object ModuleDatabaseActor {
  def props(
      moduleService: ModuleService,
      moduleViewRepository: ModuleViewRepository,
      moduleUpdatePermissionService: ModuleUpdatePermissionService,
      createNewModuleService: CreateNewModuleService,
      ctx: ExecutionContext
  ) =
    Props(
      new Impl(
        moduleService,
        moduleViewRepository,
        moduleUpdatePermissionService,
        createNewModuleService,
        ctx
      )
    )

  private final class Impl(
      moduleService: ModuleService,
      moduleViewRepository: ModuleViewRepository,
      moduleUpdatePermissionService: ModuleUpdatePermissionService,
      createNewModuleService: CreateNewModuleService,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = {
      case Handle(modules, timestamp) if modules.nonEmpty =>
        update(modules.map(_._1), timestamp).onComplete {
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

    private def update(
        modules: Seq[Module],
        timestamp: LocalDateTime
    ) =
      for {
        created <- moduleService.createOrUpdateMany(modules, timestamp)
        _       <- moduleViewRepository.refreshView()
        permissions <- moduleUpdatePermissionService.overrideInherited(
          modules.map(a =>
            (
              a.metadata.id,
              a.metadata.responsibilities.moduleManagement
            )
          )
        )
        _ <- createNewModuleService.deleteMany(modules.map(_.metadata.id))
      } yield (created, permissions)
  }
}
