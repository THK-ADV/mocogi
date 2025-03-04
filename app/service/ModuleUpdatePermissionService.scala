package service

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import cats.data.NonEmptyList
import database.repo.ModuleUpdatePermissionRepository
import models.core.Identity
import models.ModuleCore
import models.ModuleDraft
import models.ModuleUpdatePermission
import models.ModuleUpdatePermissionType
import models.ModuleUpdatePermissionType.Inherited

@Singleton
final class ModuleUpdatePermissionService @Inject() (
    private val repo: ModuleUpdatePermissionRepository,
    implicit val ctx: ExecutionContext
) {
  def overrideInherited(modules: Seq[(UUID, NonEmptyList[Identity])]) = {
    val entries = ListBuffer[(UUID, Seq[CampusId])]()
    modules.foreach {
      case (m, id) =>
        val campusIds = id.collect { case i if i.username.isDefined => CampusId(i.username.get) }
        entries.append((m, campusIds))
    }
    Future.sequence(entries.toList.map { case (m, ids) => replace(m, ids, Inherited) }).map(_ => ())
  }

  def replace(
      module: UUID,
      campusIds: Seq[CampusId],
      kind: ModuleUpdatePermissionType
  ) =
    for {
      _ <- repo.delete(module, campusIds)
      _ <- kind match
        case ModuleUpdatePermissionType.Inherited => repo.deleteInherited(module)
        case ModuleUpdatePermissionType.Granted   => repo.deleteGranted(module)
      _ <- repo.createMany(campusIds.distinct.map(c => (module, c, kind)))
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
}
