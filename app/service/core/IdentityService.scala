package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.repo.core.IdentityRepository
import models.core.Identity
import models.core.Identity.toDbEntry
import models.UserInfo
import parsing.core.IdentityFileParser
import permission.PermissionType.ApprovalFastForward
import permission.PermissionType.ArtifactsCreate
import permission.PermissionType.ArtifactsPreview
import permission.PermissionType.Module
import permission.Permissions
import play.api.libs.json.*

@Singleton
final class IdentityService @Inject() (
    val repo: IdentityRepository,
    implicit val ctx: ExecutionContext
) extends YamlService[Identity] {

  override def parser =
    Future.successful(IdentityFileParser.parser())

  override def createOrUpdateMany(xs: Seq[Identity]): Future[Seq[Identity]] =
    repo.createOrUpdateMany(xs.map(toDbEntry)).map(_ => xs)

  override def all(): Future[Seq[Identity]] =
    repo.all().map(_.map(Identity.fromDbEntry))

  def allWithImages(): Future[JsValue] =
    repo
      .allWithImages()
      .map(entries =>
        JsArray(entries.map {
          case (db, img) =>
            val id   = Identity.fromDbEntry(db)
            val json = Json.toJson(id).as[JsObject]
            json + (("imageUrl", img.fold(JsNull)(i => JsString(i.imageUrl))))
        })
      )

  def getUserInfo(userId: String, campusId: CampusId, permissions: Permissions): Future[UserInfo] =
    repo.getUserInfo(userId, campusId.value).map { userInfo =>
      // PAV or SGL grant, or ArtifactsPreview, ArtifactsCreate, Admin permission
      val hasDirectorPrivileges =
        userInfo.hasDirectorPrivileges || permissions.hasAnyPermission(ArtifactsPreview, ArtifactsCreate)
      // PAV grant, or Admin permission
      val hasModuleReviewPrivileges = userInfo.hasModuleReviewPrivileges || permissions.isAdmin
      // Has direct grant, or Module permission
      val hasModulesToEdit = userInfo.hasModulesToEdit || permissions.hasAnyPermission(Module)
      // Get directly from permissions because they are already resolved
      val fastForwardApprovalPOs = permissions.request(ApprovalFastForward)
      userInfo.copy(
        hasDirectorPrivileges = hasDirectorPrivileges,
        hasModuleReviewPrivileges = hasModuleReviewPrivileges,
        hasModulesToEdit = hasModulesToEdit,
        fastForwardApprovalPOs = fastForwardApprovalPOs,
        hasExtendedModuleEditPermissions = permissions.isAdmin
      )
    }
}
