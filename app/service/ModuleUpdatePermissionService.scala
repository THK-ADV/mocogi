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
import play.api.libs.json.JsValue
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

  def hasPermission(campusId: CampusId, module: UUID) =
    repo.hasPermission(campusId, module)

  def allFromUser(campusId: CampusId): Future[Seq[ModuleUpdatePermission]] =
    repo.allFromUser(campusId)

  @deprecated("replaced by allGrantedFromModule, which returns the user ids intead of campus id")
  def allGrantedFromModule2(moduleId: UUID): Future[Seq[CampusId]] =
    repo.allGrantedFromModule2(moduleId)

  def allGrantedFromModule(moduleId: UUID): Future[String] =
    repo.allGrantedFromModule(moduleId)

  @Deprecated(since = "the introduction of a better api: ModuleDraftRepository.allForCampusId", forRemoval = true)
  def allForCampusId(
      campusId: CampusId
  ): Future[
    Seq[((ModuleCore, Option[Double]), ModuleUpdatePermissionType, Option[ModuleDraft])]
  ] =
    repo.allForCampusId(campusId)

  private def parsePOs(roles: Set[String]) = {
    def parseAccreditationPOs(role: String): Option[List[String]] = {
      if (!role.startsWith("[") || !role.endsWith("]")) return None

      val content = role.drop(1).dropRight(1)
      if (content.isEmpty) return None

      val pos = content.split(",").map(_.trim).filter(_.nonEmpty).toList
      Option.when(pos.nonEmpty)(pos)
    }

    val prefix = "accreditation-member_"
    roles.find(_.startsWith(prefix)).flatMap(a => parseAccreditationPOs(a.drop(prefix.length)))
  }

  def allForUser(cid: CampusId, roles: Set[String]): Future[JsValue] = {
    val forUser = repo.allForUser(cid)

    parsePOs(roles) match {
      case Some(pos) =>
        for {
          forUser <- forUser
          forPo   <- repo.allForPos(pos)
        } yield {
          if forPo == "[]" then Json.obj("default" -> Json.parse(forUser))
          else Json.obj("default"                  -> Json.parse(forUser), "accreditation" -> Json.parse(forPo))
        }
      case None => forUser.map(s => Json.obj("default" -> Json.parse(s)))
    }
  }

  def isModuleInPO(module: UUID, roles: Set[String]): Future[Boolean] =
    parsePOs(roles) match {
      case Some(pos) => repo.isModuleInPO(module, pos)
      case None      => Future.successful(false)
    }
}
