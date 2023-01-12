package database.table

import models.{ModuleDraft, ModuleDraftStatus}
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID

final class ModuleDraftTable(tag: Tag)
    extends Table[ModuleDraft](tag, "module_draft") {
  def module = column[UUID]("module_id", O.PrimaryKey)

  def data = column[String]("module_json")

  def branch = column[String]("branch")

  def status = column[ModuleDraftStatus]("status")

  def lastModified = column[LocalDateTime]("last_modified")

  override def * =
    (
      module,
      data,
      branch,
      status,
      lastModified
    ) <> (ModuleDraft.tupled, ModuleDraft.unapply)
}
