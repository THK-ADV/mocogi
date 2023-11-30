package git.subscriber

import akka.actor.{Actor, Props}
import database.view.ModuleViewRepository
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import play.api.Logging
import service.{ModuleCompendiumService, ModuleUpdatePermissionService}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumDatabaseActor {
  def props(
      metadataService: ModuleCompendiumService,
      moduleViewRepository: ModuleViewRepository,
      moduleUpdatePermissionService: ModuleUpdatePermissionService,
      ctx: ExecutionContext
  ) =
    Props(
      new ModuleCompendiumDatabaseActor(
        metadataService,
        moduleViewRepository,
        moduleUpdatePermissionService,
        ctx
      )
    )
}

private final class ModuleCompendiumDatabaseActor(
    metadataService: ModuleCompendiumService,
    moduleViewRepository: ModuleViewRepository,
    moduleUpdatePermissionService: ModuleUpdatePermissionService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case CreatedOrUpdated(entries) if entries.nonEmpty =>
      val res = for {
        mcs <- metadataService.createOrUpdateMany(entries)
        _ <- moduleViewRepository.refreshView()
        perms <- moduleUpdatePermissionService.createOrUpdateInherited(
          entries.map(a =>
            (a._2.metadata.id, a._2.metadata.responsibilities.moduleManagement)
          )
        )
      } yield (mcs, perms)

      res onComplete {
        case Success((mcs, perms)) =>
          logger.info(
            s"successfully created or updated ${mcs.size} mc and ${perms.size} permission entries"
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
