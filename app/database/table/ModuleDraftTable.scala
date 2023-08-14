package database.table

import models._
import play.api.libs.json.JsValue
import service.Print
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID

final class ModuleDraftTable(tag: Tag)
    extends Table[ModuleDraft](tag, "module_draft") {
  def module = column[UUID]("module_id", O.PrimaryKey)

  def user = column[User]("user_id", O.PrimaryKey)

  def branch = column[Branch]("branch_id")

  def status = column[ModuleDraftStatus]("status")

  def data = column[JsValue]("module_json")

  def moduleCompendium =
    column[JsValue]("module_compendium_json")

  def moduleCompendiumPrint = column[Print]("module_compendium_print")

  def keysToBeReviewed = column[Option[List[String]]]("keys_to_be_reviewed")

  def lastCommit = column[Option[CommitId]]("last_commit_id")

  def mergeRequestId = column[Option[MergeRequestId]]("merge_request_id")

  def mergeRequestAuthor = column[Option[User]]("merge_request_id")

  def lastModified = column[LocalDateTime]("last_modified")

  override def * =
    (
      module,
      user,
      branch,
      status,
      data,
      moduleCompendium,
      moduleCompendiumPrint,
      keysToBeReviewed,
      lastCommit,
      mergeRequestId,
      mergeRequestAuthor,
      lastModified
    ) <> (mapRow, unmapRow)

  private def mapRow: (
      (
          UUID,
          User,
          Branch,
          ModuleDraftStatus,
          JsValue,
          JsValue,
          Print,
          Option[List[String]],
          Option[CommitId],
          Option[MergeRequestId],
          Option[User],
          LocalDateTime
      )
  ) => ModuleDraft = {
    case (
          module,
          user,
          branch,
          status,
          data,
          moduleCompendium,
          moduleCompendiumPrint,
          keysToBeReviewed,
          lastCommit,
          mergeRequestId,
          mergeRequestAuthor,
          lastModified
        ) =>
      ModuleDraft(
        module,
        user,
        branch,
        status,
        data,
        moduleCompendium,
        moduleCompendiumPrint,
        keysToBeReviewed,
        lastCommit,
        mergeRequestId.zip(mergeRequestAuthor),
        lastModified
      )
  }

  private def unmapRow: ModuleDraft => Option[
    (
        UUID,
        User,
        Branch,
        ModuleDraftStatus,
        JsValue,
        JsValue,
        Print,
        Option[List[String]],
        Option[CommitId],
        Option[MergeRequestId],
        Option[User],
        LocalDateTime
    )
  ] = { a =>
    Option(
      (
        a.module,
        a.user,
        a.branch,
        a.status,
        a.data,
        a.moduleCompendium,
        a.print,
        a.keysToBeReviewed,
        a.lastCommit,
        a.mergeRequest.map(_._1),
        a.mergeRequest.map(_._2),
        a.lastModified
      )
    )
  }
}
