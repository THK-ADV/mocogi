package database.table

import models.ModuleReviewStatus
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleReviewTable(tag: Tag)
    extends Table[(UUID, ModuleReviewStatus)](
      tag,
      "module_review"
    ) {

  def moduleDraft = column[UUID]("module_draft", O.PrimaryKey)

  def status = column[ModuleReviewStatus]("status")

  def moduleDraftFk =
    foreignKey("moduleDraft", moduleDraft, TableQuery[ModuleDraftTable])(
      _.module
    )

  override def * = (moduleDraft, status)
}
