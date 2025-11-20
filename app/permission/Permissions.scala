package permission

import permission.PermissionType.Admin
import permission.PermissionType.ApprovalFastForward
import permission.PermissionType.ArtifactsCreate
import permission.PermissionType.ArtifactsPreview

enum PermissionType(val label: String) {
  case Module              extends PermissionType("module")
  case ApprovalFastForward extends PermissionType("approval-fast-forward")
  case Admin               extends PermissionType("admin")
  case ArtifactsPreview    extends PermissionType("artifacts-preview")
  case ArtifactsCreate     extends PermissionType("artifacts-create")

  def isAdmin: Boolean = this == Admin
}

object PermissionType {
  def apply(label: String): PermissionType =
    label match {
      case "module"                => Module
      case "approval-fast-forward" => ApprovalFastForward
      case "admin"                 => Admin
      case "artifacts-preview"     => ArtifactsPreview
      case "artifacts-create"      => ArtifactsCreate
    }
}

case class Permissions(private val permissions: Map[PermissionType, Set[String]]) extends AnyVal {
  def isAdmin: Boolean = this.permissions.contains(Admin)

  def get(action: PermissionType): Option[Set[String]] =
    permissions.get(action)

  def request(action: PermissionType): Option[Set[String]] =
    permissions.get(Admin).orElse(permissions.get(action))

  def modulePermissions: Option[Set[String]] =
    request(PermissionType.Module)

  def approvalFastForwardPermissions: Option[Set[String]] =
    request(ApprovalFastForward)

  def hasAnyPermission(perms: PermissionType*): Boolean =
    permissions.contains(Admin) || perms.exists(permissions.contains)

  def artifactsCreatePermissions: Set[String] =
    permissions.getOrElse(ArtifactsCreate, Set.empty)

  def artifactsPreviewPermissions: Set[String] = {
    val pos = scala.collection.mutable.Set[String]()
    permissions.get(ArtifactsPreview).foreach(pos.addAll)
    permissions.get(ArtifactsCreate).foreach(pos.addAll)
    pos.toSet
  }
}
