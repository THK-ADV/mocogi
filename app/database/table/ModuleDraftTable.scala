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

  def moduleTitle = column[String]("module_title")

  def moduleAbbrev = column[String]("module_abbrev")

  def author = column[String]("author")

  def branch = column[Branch]("branch")

  def source = column[ModuleDraftSource]("source")

  def data = column[JsValue]("module_json")

  def moduleCompendium = column[JsValue]("module_compendium_json")

  def moduleCompendiumPrint = column[Print]("module_compendium_print")

  def keysToBeReviewed = column[Set[String]]("keys_to_be_reviewed")

  def modifiedKeys = column[Set[String]]("modified_keys")

  def lastCommit = column[Option[CommitId]]("last_commit_id")

  def mergeRequestId = column[Option[MergeRequestId]]("merge_request_id")

  def mergeRequestStatus =
    column[Option[MergeRequestStatus]]("merge_request_status")

  def lastModified = column[LocalDateTime]("last_modified")

  def authorFk =
    foreignKey("author", author, TableQuery[PersonTable])(
      _.id
    )

  override def * =
    (
      module,
      moduleTitle,
      moduleAbbrev,
      author,
      branch,
      source,
      data,
      moduleCompendium,
      moduleCompendiumPrint,
      keysToBeReviewed,
      modifiedKeys,
      lastCommit,
      mergeRequestId,
      mergeRequestStatus,
      lastModified
    ) <> (mapRow, unmapRow)

  def mapRow: (
      (
          UUID,
          String,
          String,
          String,
          Branch,
          ModuleDraftSource,
          JsValue,
          JsValue,
          Print,
          Set[String],
          Set[String],
          Option[CommitId],
          Option[MergeRequestId],
          Option[MergeRequestStatus],
          LocalDateTime
      )
  ) => ModuleDraft = {
    case (
          module,
          moduleTitle,
          moduleAbbrev,
          author,
          branch,
          source,
          data,
          moduleCompendium,
          moduleCompendiumPrint,
          keysToBeReviewed,
          modifiedKeys,
          lastCommit,
          mergeRequestId,
          mergeRequestStatus,
          lastModified
        ) =>
      ModuleDraft(
        module,
        moduleTitle,
        moduleAbbrev,
        author,
        branch,
        source,
        data,
        moduleCompendium,
        moduleCompendiumPrint,
        keysToBeReviewed,
        modifiedKeys,
        lastCommit,
        mergeRequestId.zip(mergeRequestStatus),
        lastModified
      )
  }

  def unmapRow: ModuleDraft => Option[
    (
        UUID,
        String,
        String,
        String,
        Branch,
        ModuleDraftSource,
        JsValue,
        JsValue,
        Print,
        Set[String],
        Set[String],
        Option[CommitId],
        Option[MergeRequestId],
        Option[MergeRequestStatus],
        LocalDateTime
    )
  ] = d =>
    Option(
      (
        d.module,
        d.moduleTitle,
        d.moduleAbbrev,
        d.author,
        d.branch,
        d.source,
        d.data,
        d.moduleCompendium,
        d.print,
        d.keysToBeReviewed,
        d.modifiedKeys,
        d.lastCommit,
        d.mergeRequest.map(_._1),
        d.mergeRequest.map(_._2),
        d.lastModified
      )
    )
}
