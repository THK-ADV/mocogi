package database.table

import java.time.LocalDateTime
import java.util.UUID

import database.table.core.IdentityTable
import git.Branch
import git.CommitId
import git.MergeRequestId
import git.MergeRequestStatus
import models.*
import play.api.libs.json.JsValue
import service.Print
import slick.jdbc.PostgresProfile.api.*

object ModuleDraftTable {
  given BaseColumnType[Set[String]] =
    MappedColumnType
      .base[Set[String], String](
        xs => if (xs.isEmpty) "" else xs.mkString(","),
        s => {
          if (s.isEmpty) Set.empty
          else
            s.split(",").foldLeft(Set.empty[String]) {
              case (acc, s) =>
                acc.+(s)
            }
        }
      )
}

final class ModuleDraftTable(tag: Tag) extends Table[ModuleDraft](tag, "module_draft") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
  import ModuleDraftTable.given_BaseColumnType_Set

  def module = column[UUID]("module", O.PrimaryKey)

  def moduleTitle = column[String]("module_title")

  def moduleAbbrev = column[String]("module_abbrev")

  def author = column[String]("author")

  def branch = column[Branch]("branch")

  def source = column[ModuleDraftSource]("source")

  def moduleJson = column[JsValue]("module_json")

  def moduleJsonValidated = column[JsValue]("module_json_validated")

  def modulePrint = column[Print]("module_print")

  def keysToBeReviewed = column[Set[String]]("keys_to_be_reviewed")

  def modifiedKeys = column[Set[String]]("modified_keys")

  def lastCommit = column[Option[CommitId]]("last_commit_id")

  def mergeRequestId = column[Option[MergeRequestId]]("merge_request_id")

  def mergeRequestStatus =
    column[Option[MergeRequestStatus]]("merge_request_status")

  def lastModified = column[LocalDateTime]("last_modified")

  def authorFk =
    foreignKey("author", author, TableQuery[IdentityTable])(
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
      moduleJson,
      moduleJsonValidated,
      modulePrint,
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
          moduleJson,
          moduleJsonValidated,
          modulePrint,
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
        moduleJson,
        moduleJsonValidated,
        modulePrint,
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
        d.moduleJson,
        d.moduleJsonValidated,
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
