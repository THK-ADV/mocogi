package models

import auth.CampusId
import play.api.libs.json.{Json, Writes}

import java.util.UUID

case class ModuleUpdatePermission(
    moduleId: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    campusId: CampusId,
    moduleUpdatePermissionType: ModuleUpdatePermissionType
)

object ModuleUpdatePermission {
  implicit def writes: Writes[ModuleUpdatePermission] =
    Json.writes[ModuleUpdatePermission]
}
