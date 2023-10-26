package database.table

import models.{ModuleReview, ModuleReviewStatus}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleReviewTable(tag: Tag)
    extends Table[ModuleReview](
      tag,
      "module_review"
    ) {

  def moduleDraft = column[UUID]("module_draft", O.PrimaryKey)

  def status = column[ModuleReviewStatus]("status")

  def moduleDraftFk =
    foreignKey("moduleDraft", moduleDraft, TableQuery[ModuleDraftTable])(
      _.module
    )

  override def * = (moduleDraft, status) <> (mapRow, unmapRow)

  def mapRow: ((UUID, ModuleReviewStatus)) => ModuleReview = {
    case (moduleDraft, status) =>
      ModuleReview(moduleDraft, status, Nil)
  }

  def unmapRow: ModuleReview => Option[(UUID, ModuleReviewStatus)] = r =>
    Some((r.moduleDraft, r.status))
}
