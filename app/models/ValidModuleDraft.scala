package models

import play.api.libs.json.JsValue
import service.Print

import java.time.LocalDateTime
import java.util.UUID

case class ValidModuleDraft(
    module: UUID,
    status: ModuleDraftStatus,
    lastModified: LocalDateTime,
    json: JsValue,
    print: Print
)
