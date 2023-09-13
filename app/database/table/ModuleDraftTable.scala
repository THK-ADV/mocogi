package database.table

import models._
import play.api.libs.json.JsValue
import service.Print
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID

final class ModuleDraftTable(tag: Tag)
    extends Table[ModuleDraft](tag, "module_draft") {
  def module = column[UUID]("module", O.PrimaryKey)

  def user = column[User]("user_id")

  def branch = column[Branch]("branch")

  def status = column[ModuleDraftSource]("status")

  def data = column[JsValue]("module_json")

  def moduleCompendium =
    column[JsValue]("module_compendium_json")

  def moduleCompendiumPrint = column[Print]("module_compendium_print")

  def keysToBeReviewed = column[Set[String]]("keys_to_be_reviewed")

  def modifiedKeys = column[Set[String]]("modified_keys")

  def lastCommit = column[Option[CommitId]]("last_commit_id")

  def mergeRequestId = column[Option[MergeRequestId]]("merge_request_id")

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
      modifiedKeys,
      lastCommit,
      mergeRequestId,
      lastModified
    ) <> ((ModuleDraft.apply _).tupled, ModuleDraft.unapply)
}
