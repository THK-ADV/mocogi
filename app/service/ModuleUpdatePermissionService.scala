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
import models.ModuleUpdatePermissionType
import models.ModuleUpdatePermissionType.Inherited
import permission.Permissions
import play.api.libs.json.JsObject
import play.api.libs.json.Json

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

  def hasPermissionFor(module: UUID, campusId: CampusId) =
    repo.hasPermission(campusId, module)

  def isAuthorOf(moduleId: UUID, personId: String) =
    repo.isAuthorOf(moduleId, personId)

  def allGrantedFromModule(moduleId: UUID): Future[String] =
    repo.allGrantedFromModule(moduleId)

  /**
   * Fetch all modules that can be edited by the user through inherited (MV) or granted permission.
   * Then fetch all modules that can be edited by a role.
   * @return The combination of the two module-lists as a JSON object
   */
  def allModulesForUser(cid: CampusId, permissions: Permissions): Future[JsObject] = {
    val forUser = repo.allForUser(cid).map(s => Json.obj("direct" -> Json.parse(s)))

    permissions.modulePermissions match {
      case Some(pos) if pos.nonEmpty =>
        for {
          forUser <- forUser
          forPo   <- repo.allForPos(pos)
        } yield {
          if forPo == "[]" then forUser
          else forUser + ("indirect" -> Json.parse(forPo))
        }
      case _ => forUser
    }
  }

  def isModulePartOfPO(module: UUID, pos: Set[String]): Future[Boolean] =
    repo.isModulePartOfPO(module, pos)
}
