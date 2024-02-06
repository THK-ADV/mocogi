package git.subscriber

import akka.actor.{Actor, Props}
import database.view.ModuleViewRepository
import git.subscriber.ModuleSubscribers.CreatedOrUpdated
import play.api.Logging
import service.{ModuleService, ModuleUpdatePermissionService}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleDatabaseActor {
  def props(
      moduleService: ModuleService,
      moduleViewRepository: ModuleViewRepository,
      moduleUpdatePermissionService: ModuleUpdatePermissionService,
      ctx: ExecutionContext
  ) =
    Props(
      new Impl(
        moduleService,
        moduleViewRepository,
        moduleUpdatePermissionService,
        ctx
      )
    )

  private final class Impl(
      moduleService: ModuleService,
      moduleViewRepository: ModuleViewRepository,
      moduleUpdatePermissionService: ModuleUpdatePermissionService,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = {
      case CreatedOrUpdated(entries) if entries.nonEmpty =>
        val res = for {
          modules <- moduleService.createOrUpdateMany(entries)
          _ <- moduleViewRepository.refreshView()
          permissions <- moduleUpdatePermissionService.createOrUpdateInherited(
            entries.map(a =>
              (
                a._2.metadata.id,
                a._2.metadata.responsibilities.moduleManagement
              )
            )
          )
        } yield (modules, permissions)

        res onComplete {
          case Success((modules, permissions)) =>
            logger.info(
              s"successfully created or updated ${modules.size} mc and ${permissions.size} permission entries"
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
  }
}
