package models

import play.api.libs.json.JsValue
import service.Print

import java.time.LocalDateTime
import java.util.UUID

case class ModuleDraft(
    module: UUID,
    data: String,
    branch: String,
    status: ModuleDraftStatus,
    lastModified: LocalDateTime,
    validation: Option[Either[JsValue, (JsValue, Print)]]
)

case class ModuleDraftProtocol(
    data: String,
    branch: String
)