package models

import play.api.libs.json.JsValue
import service.Print

import java.time.LocalDateTime
import java.util.UUID

case class ModuleDraft(
    module: UUID,
    user: User,
    branch: Branch,
    status: ModuleDraftStatus,
    data: JsValue,
    moduleCompendium: JsValue,
    print: Print,
    keysToBeReviewed: Option[List[String]],
    lastCommit: Option[CommitId],
    mergeRequest: Option[(MergeRequestId, User)],
    lastModified: LocalDateTime
)

case class ModuleDraftProtocol(
    data: ModuleCompendiumProtocol,
    branch: String
)
