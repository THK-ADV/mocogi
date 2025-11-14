package auth

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

case class Permissions(permissions: Map[PermissionType, Seq[String]]) extends AnyVal {
  def isAdmin: Boolean = this.permissions.contains(PermissionType.Admin)

  def request(action: PermissionType): Option[Seq[String]] =
    permissions.get(PermissionType.Admin).orElse(permissions.get(action))

  def modulePermissions: Option[Seq[String]] =
    request(PermissionType.Module)

  def approvalFastForwardPermissions: Option[Seq[String]] =
    request(PermissionType.ApprovalFastForward)

  def hasPermission(perms: PermissionType*): Boolean =
    permissions.contains(PermissionType.Admin) || perms.exists(permissions.contains)
}
