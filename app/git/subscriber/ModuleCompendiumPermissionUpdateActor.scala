package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleCompendiumSubscribers.CreatedOrUpdated
import models.core.Person
import models.{ModuleUpdatePermissionType, CampusId}
import play.api.Logging
import service.ModuleUpdatePermissionService

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumPermissionUpdateActor {
  def props(service: ModuleUpdatePermissionService, ctx: ExecutionContext) =
    Props(new ModuleCompendiumPermissionUpdateActor(service, ctx))
}

private final class ModuleCompendiumPermissionUpdateActor(
    service: ModuleUpdatePermissionService,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case CreatedOrUpdated(entries) if entries.nonEmpty =>
      updatePermissions(
        entries.map(a =>
          (a._2.metadata.id, a._2.metadata.responsibilities.moduleManagement)
        )
      )
  }

  private def updatePermissions(modules: Seq[(UUID, List[Person])]): Unit =
    service.createOrUpdateInherited(modules) onComplete {
      case Success(permissions) =>
        logSuccess(permissions)
      case Failure(e) =>
        logError(e)
    }

  private def logSuccess(
      xs: Seq[(UUID, CampusId, ModuleUpdatePermissionType)]
  ): Unit =
    logger.info(
      s"""successfully created or updated module permissions
         |  - permissions: ${xs.mkString("\n    ")}""".stripMargin
    )

  private def logError(throwable: Throwable): Unit =
    logger.error(
      s"""failed to update module permissions
         |  - message: ${throwable.getMessage}
         |  - trace: ${throwable.getStackTrace.mkString(
          "\n           "
        )}""".stripMargin
    )
}
