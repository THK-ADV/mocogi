package permission

import permission.PermissionType.Admin
import permission.PermissionType.ArtifactsCreate
import permission.PermissionType.ArtifactsPreview
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads

enum PermissionType(val id: String) {
  case Module              extends PermissionType("module")
  case ApprovalFastForward extends PermissionType("approval-fast-forward")
  case Admin               extends PermissionType("admin")
  case ArtifactsPreview    extends PermissionType("artifacts-preview")
  case ArtifactsCreate     extends PermissionType("artifacts-create")
  case SchedulePlanning    extends PermissionType("schedule-planning")
  case ScheduleBooking     extends PermissionType("schedule-booking")

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
      case "schedule-planning"     => SchedulePlanning
      case "schedule-booking"      => ScheduleBooking
    }

  given Format[PermissionType] = Format(
    js => js.\("id").validate[String].map(apply),
    p => {
      val label = p match {
        case PermissionType.Module              => "Module der PO bearbeiten"
        case PermissionType.ApprovalFastForward => "Review überspringen"
        case PermissionType.Admin               => "Admin"
        case PermissionType.ArtifactsPreview    => "Artefakte anzeigen"
        case PermissionType.ArtifactsCreate     => "Artefakte erstellen"
        case PermissionType.SchedulePlanning    => "Stundenplanung"
        case PermissionType.ScheduleBooking     => "Einzelbuchung"
      }
      Json.obj("id" -> p.id, "label" -> label)
    }
  )
}

case class Permissions(private val permissions: Map[PermissionType, Set[String]]) extends AnyVal {
  def isAdmin: Boolean = this.permissions.contains(Admin)

  def get(action: PermissionType): Option[Set[String]] =
    permissions.get(action)

  def request(action: PermissionType): Option[Set[String]] =
    permissions.get(Admin).orElse(permissions.get(action))

  def modulePermissions: Option[Set[String]] =
    request(PermissionType.Module)

  def hasAnyPermission(perms: PermissionType*): Boolean =
    permissions.contains(Admin) || perms.exists(permissions.contains)

  def hasScheduleBooking: Boolean =
    hasAnyPermission(PermissionType.SchedulePlanning, PermissionType.ScheduleBooking)

  def artifactsCreatePermissions: Set[String] =
    permissions.getOrElse(ArtifactsCreate, Set.empty)

  def artifactsPreviewPermissions: Set[String] = {
    val pos = scala.collection.mutable.Set[String]()
    permissions.get(ArtifactsPreview).foreach(pos.addAll)
    permissions.get(ArtifactsCreate).foreach(pos.addAll)
    pos.toSet
  }
}

case class Permission(permType: PermissionType, context: Option[List[String]], person: String)

object Permission {
  given Reads[Permission] = Json.reads[Permission]
}
