package service

import controllers.formats.ModuleCompendiumProtocolFormat
import database.repo.ModuleUpdatePermissionRepository
import models.ModuleUpdatePermissionType.{Granted, Inherited}
import models.core.Person
import models.{
  CampusId,
  Module,
  ModuleCompendiumProtocol,
  ModuleUpdatePermission
}
import play.api.libs.json.Json

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleUpdatePermissionService @Inject() (
    private val repo: ModuleUpdatePermissionRepository,
    private implicit val ctx: ExecutionContext
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

  def createGranted(module: UUID, campusId: CampusId) =
    repo.create((module, campusId, Granted))

  def removeGranted(module: UUID, campusId: CampusId) =
    repo.delete(module, campusId, Granted)

  def hasPermission(campusId: CampusId, module: UUID) =
    repo.hasPermission(campusId, module)

  def all(campusId: CampusId): Future[Seq[ModuleUpdatePermission]] =
    repo
      .allWithModule(campusId)
      .map(_.map { case (id, campusId, kind, module) =>
        val (title, abbrev) = module match {
          case Left(value) => value
          case Right(js) =>
            val p = Json.fromJson[ModuleCompendiumProtocol](js).get
            (p.metadata.title, p.metadata.abbrev)
        }
        ModuleUpdatePermission(id, title, abbrev, campusId, kind)
      })

  def hasInheritedPermission(
      campusId: CampusId,
      module: UUID
  ): Future[Boolean] =
    repo.hasInheritedPermission(campusId, module)

  def allForCampusId(campusId: CampusId): Future[Seq[Module]] =
    repo.allForCampusId(campusId)
}
