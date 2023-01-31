package models

import java.time.LocalDateTime
import java.util.UUID

case class ModuleDraft(
    module: UUID,
    data: String,
    branch: String,
    status: ModuleDraftStatus,
    lastModified: LocalDateTime,
    validation: Option[Either[String, (String, String)]]
)

case class ModuleDraftProtocol(
    data: String,
    branch: String
)