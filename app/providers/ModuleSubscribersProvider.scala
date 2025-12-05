package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.view.ModuleViewRepository
import git.subscriber.*
import org.apache.pekko.actor.ActorSystem
import service.ModuleCreationService
import service.ModuleService
import service.ModuleUpdatePermissionService

@Singleton
final class ModuleSubscribersProvider @Inject() (
    system: ActorSystem,
    metadataService: ModuleService,
    moduleViewRepository: ModuleViewRepository,
    moduleUpdatePermissionService: ModuleUpdatePermissionService,
    ctx: ExecutionContext,
    moduleCreationService: ModuleCreationService,
) extends Provider[ModuleSubscribers] {
  override def get(): ModuleSubscribers =
    ModuleSubscribers(
      List(
        system.actorOf(
          ModuleDatabaseActor
            .props(
              metadataService,
              moduleViewRepository,
              moduleUpdatePermissionService,
              moduleCreationService,
              ctx
            )
        )
      )
    )
}
