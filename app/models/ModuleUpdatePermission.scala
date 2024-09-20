package models

import java.util.UUID

import auth.CampusId
import play.api.libs.json.Json
import play.api.libs.json.Writes

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
