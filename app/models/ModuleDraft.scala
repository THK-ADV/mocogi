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
import service.pipeline.Print

case class ModuleDraft(
    module: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    author: String,
    branch: Branch,
    source: ModuleDraftSource,
    moduleJson: JsValue,
    moduleJsonValidated: JsValue,
    print: Print,
    keysToBeReviewed: Set[String],
    modifiedKeys: Set[String],
    lastCommit: Option[CommitId],
    mergeRequest: Option[(MergeRequestId, MergeRequestStatus)],
    lastModified: LocalDateTime
) {
  def protocol(): ModuleProtocol =
    ModuleJson.reads.reads(this.moduleJson) match {
      case JsSuccess(value, _) => value.toProtocol
      case JsError(_)          =>
        throw new Exception(
          s"Unable to parse module draft with ID ${this.module} to JSON"
        )
    }

  def mergeRequestId: Option[MergeRequestId] =
    this.mergeRequest.map(_._1)

  def mergeRequestStatus: Option[MergeRequestStatus] =
    this.mergeRequest.map(_._2)

  // changes to the module draft state calculation have to be synchronized with the "get_modules_for_user" function in functions.sql

  def state(): ModuleDraftState =
    if (
      this.lastCommit.isDefined &&
      this.mergeRequest.isEmpty &&
      this.modifiedKeys.nonEmpty &&
      this.keysToBeReviewed.isEmpty
    ) ValidForPublication
    else if (
      this.lastCommit.isDefined &&
      this.mergeRequest.isEmpty &&
      this.modifiedKeys.nonEmpty &&
      this.keysToBeReviewed.nonEmpty
    ) ValidForReview
    else if (
      this.lastCommit.isDefined &&
      this.keysToBeReviewed.nonEmpty &&
      this.mergeRequestStatus.contains(Open)
    ) WaitingForReview
    else if (
      this.lastCommit.isDefined &&
      this.keysToBeReviewed.isEmpty &&
      this.mergeRequestStatus.contains(Open)
    ) WaitingForPublication
    else if (
      this.lastCommit.isDefined &&
      this.mergeRequestStatus.contains(Closed)
    ) WaitingForChanges
    else Unknown
}

extension (self: Option[ModuleDraft]) {
  def state() =
    self.fold[ModuleDraftState](Published)(_.state())
}
