package models

import controllers.formats.ModuleCompendiumProtocolFormat
import play.api.libs.json.{JsValue, Json, Writes}
import service.Print

import java.time.LocalDateTime
import java.util.UUID

case class ModuleDraft(
    module: UUID,
    user: User,
    branch: Branch,
    source: ModuleDraftSource,
    data: JsValue,
    moduleCompendium: JsValue,
    print: Print,
    keysToBeReviewed: Set[String],
    modifiedKeys: Set[String],
    lastCommit: Option[CommitId],
    mergeRequest: Option[(MergeRequestId, MergeRequestStatus)],
    lastModified: LocalDateTime
)

object ModuleDraft extends ModuleCompendiumProtocolFormat {
  implicit val moduleDraftFmt: Writes[ModuleDraft] =
    Writes.apply(d =>
      Json.obj(
        "module" -> d.module,
        "user" -> d.user.username,
        "status" -> d.source,
        "data" -> d.data,
        "keysToBeReviewed" -> d.keysToBeReviewed,
        "mergeRequestId" -> d.mergeRequest.map(_._1.value),
        "lastModified" -> d.lastModified
      )
    )

  final implicit class Ops(private val self: ModuleDraft) extends AnyVal {
    def protocol(): ModuleCompendiumProtocol =
      Json.fromJson(self.data).get

    def mergeRequestId: Option[MergeRequestId] =
      self.mergeRequest.map(_._1)

    def mergeRequestStatus: Option[MergeRequestStatus] =
      self.mergeRequest.map(_._2)
  }
}
