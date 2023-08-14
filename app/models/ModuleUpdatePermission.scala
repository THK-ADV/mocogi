package models

import java.util.UUID

case class ModuleUpdatePermission(
    moduleId: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    user: User,
    moduleUpdatePermissionType: ModuleUpdatePermissionType
)
