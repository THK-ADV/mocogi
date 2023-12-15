package models

import controllers.formats.ModuleCompendiumProtocolFormat
import models.MergeRequestStatus.{Closed, Open}
import models.ModuleDraftState.{
  Published,
  Unknown,
  ValidForPublication,
  ValidForReview,
  WaitingForChanges,
  WaitingForReview
}
import play.api.libs.json.{JsValue, Json}
import service.Print

import java.time.LocalDateTime
import java.util.UUID

case class ModuleDraft(
    module: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    author: String,
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
  final implicit class Ops(private val self: ModuleDraft) extends AnyVal {
    def protocol(): ModuleCompendiumProtocol =
      Json.fromJson(self.data).get

    def mergeRequestId: Option[MergeRequestId] =
      self.mergeRequest.map(_._1)

    def mergeRequestStatus: Option[MergeRequestStatus] =
      self.mergeRequest.map(_._2)

    def state(): ModuleDraftState =
      if (
        self.lastCommit.isDefined &&
        self.mergeRequest.isEmpty &&
        self.modifiedKeys.nonEmpty &&
        self.keysToBeReviewed.isEmpty
      ) ValidForPublication
      else if (
        self.lastCommit.isDefined &&
        self.mergeRequest.isEmpty &&
        self.modifiedKeys.nonEmpty &&
        self.keysToBeReviewed.nonEmpty
      ) ValidForReview
      else if (
        self.lastCommit.isDefined &&
        self.mergeRequestStatus.contains(Open)
      ) WaitingForReview
      else if (
        self.lastCommit.isDefined &&
        self.mergeRequestStatus.contains(Closed)
      ) WaitingForChanges
      else Unknown
  }

  final implicit class OptionOps(private val self: Option[ModuleDraft])
      extends AnyVal {
    def state() =
      self.fold[ModuleDraftState](Published)(_.state())
  }
}
