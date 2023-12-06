package service

import controllers.formats.ModuleCompendiumProtocolFormat
import database.repo.ModuleUpdatePermissionRepository
import models.ModuleUpdatePermissionType.{Granted, Inherited}
import models.core.Person
import models.{
  CampusId,
  Module,
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
) extends ModuleCompendiumProtocolFormat {
  def createOrUpdateInherited(modules: Seq[(UUID, List[Person])]) = {
    def entries() =
      modules.flatMap { case (module, management) =>
        management.collect {
          case p: Person.Default if p.username.isDefined =>
            (module, CampusId(p.username.get), Inherited)
        }
      }
    for {
      _ <- repo.deleteByModules(modules.map(_._1), Inherited)
      created <- repo.createMany(entries().toList)
    } yield created
  }

  def replace(module: UUID, campusIds: List[CampusId]) =
    for {
      _ <- repo.deleteByModules(Seq(module), Granted)
      _ <- repo.createMany(campusIds.map(c => (module, c, Granted)))
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
  ): Future[Seq[(ModuleUpdatePermissionType, Module)]] =
    repo.allForCampusId(campusId)
}
