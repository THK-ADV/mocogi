package service

import auth.CampusId
import cats.data.NonEmptyList
import database.repo.ModuleUpdatePermissionRepository
import models.ModuleUpdatePermissionType.Inherited
import models.core.Identity
import models.{
  ModuleCore,
  ModuleDraft,
  ModuleUpdatePermission,
  ModuleUpdatePermissionType
}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleUpdatePermissionService @Inject() (
    private val repo: ModuleUpdatePermissionRepository,
    implicit val ctx: ExecutionContext
) {
  def overrideInherited(modules: Seq[(UUID, NonEmptyList[Identity])]) = {
    def entries() =
      modules.flatMap { case (module, management) =>
        management.collect {
          case p: Identity.Person if p.username.isDefined =>
            (module, CampusId(p.username.get), Inherited)
        }
      }
    for {
      _ <- repo.deleteByModules(modules.map(_._1), Inherited)
      created <- repo.createMany(entries().toList)
    } yield created
  }

  def replace(
      module: UUID,
      campusIds: Seq[CampusId],
      kind: ModuleUpdatePermissionType
  ) =
    for {
      _ <- repo.deleteByModules(Seq(module), kind)
      _ <- repo.createMany(campusIds.map(c => (module, c, kind)))
    } yield ()

  def hasPermission(campusId: CampusId, module: UUID) =
    repo.hasPermission(campusId, module)

  def allFromUser(campusId: CampusId): Future[Seq[ModuleUpdatePermission]] =
    repo.allFromUser(campusId)

  def allGrantedFromModule(moduleId: UUID): Future[Seq[CampusId]] =
    repo.allGrantedFromModule(moduleId)

  def hasInheritedPermission(
      campusId: CampusId,
      module: UUID
  ): Future[Boolean] =
    repo.hasInheritedPermission(campusId, module)

  def allForCampusId(
      campusId: CampusId
  ): Future[
    Seq[(ModuleCore, ModuleUpdatePermissionType, Option[ModuleDraft])]
  ] =
    repo.allForCampusId(campusId)

  def delete(module: UUID) =
    repo.delete(module)
}
