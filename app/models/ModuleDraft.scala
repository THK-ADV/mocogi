package models

import java.time.LocalDateTime
import java.util.UUID

import controllers.json.ModuleJson
import git.Branch
import git.CommitId
import git.MergeRequestId
import git.MergeRequestStatus
import git.MergeRequestStatus.Closed
import git.MergeRequestStatus.Open
import models.ModuleDraftState.Published
import models.ModuleDraftState.Unknown
import models.ModuleDraftState.ValidForPublication
import models.ModuleDraftState.ValidForReview
import models.ModuleDraftState.WaitingForChanges
import models.ModuleDraftState.WaitingForPublication
import models.ModuleDraftState.WaitingForReview
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import service.Print

case class ModuleDraft(
    module: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    author: String,
    branch: Branch,
    source: ModuleDraftSource,
    data: JsValue,
    validated: JsValue, // TODO unused
    print: Print,       // TODO unused
    keysToBeReviewed: Set[String],
    modifiedKeys: Set[String],
    lastCommit: Option[CommitId],
    mergeRequest: Option[(MergeRequestId, MergeRequestStatus)],
    lastModified: LocalDateTime
)

object ModuleDraft {
  final implicit class Ops(private val self: ModuleDraft) extends AnyVal {
    def protocol(): ModuleProtocol =
      ModuleJson.reads.reads(self.data) match {
        case JsSuccess(value, _) => value.toProtocol
        case JsError(_) =>
          throw new Exception(
            s"Unable to parse module draft with ID ${self.module} to JSON"
          )
      }

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
        self.keysToBeReviewed.nonEmpty &&
        self.mergeRequestStatus.contains(Open)
      ) WaitingForReview
      else if (
        self.lastCommit.isDefined &&
        self.keysToBeReviewed.isEmpty &&
        self.mergeRequestStatus.contains(Open)
      ) WaitingForPublication
      else if (
        self.lastCommit.isDefined &&
        self.mergeRequestStatus.contains(Closed)
      ) WaitingForChanges
      else Unknown
  }

  final implicit class OptionOps(private val self: Option[ModuleDraft]) extends AnyVal {
    def state() =
      self.fold[ModuleDraftState](Published)(_.state())
  }
}
