package service

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.IdentityRepository
import database.repo.CreatedModuleRepository
import models.CreatedModule
import models.ModuleCore
import models.ModuleUpdatePermissionType

@Singleton
final class ModuleCreationService @Inject() (
    private val repo: CreatedModuleRepository,
    private val permissionService: ModuleUpdatePermissionService,
    private val identityRepo: IdentityRepository,
    private implicit val ctx: ExecutionContext,
) {
  def createWithPermissions(module: CreatedModule): Future[Unit] =
    repo.create(module).flatMap(_ => updateModuleManagement(module.module, module.moduleManagement))

  def updateModuleManagement(module: UUID, moduleManagement: List[String]): Future[Unit] =
    for
      campusIds <- identityRepo.getCampusIds(moduleManagement)
      _         <- permissionService.replace(module, campusIds, ModuleUpdatePermissionType.Inherited)
    yield ()

  def createManyWithPermissions(modules: List[CreatedModule]): Future[Unit] =
    Future.sequence(modules.map(createWithPermissions)).map(_ => ())

  def allAsModuleCore(): Future[Seq[ModuleCore]] =
    repo.allAsModuleCore()

  def allGenericWithPOsAsModuleCore(): Future[Seq[(ModuleCore, Seq[String])]] =
    repo.allGenericWithPOsAsModuleCore()

  def deleteMany(modules: Seq[UUID]): Future[Int] =
    repo.delete(modules)

  def allGeneric(): Future[Seq[ModuleCore]] =
    repo.allGeneric()
}
