package database.table

import git.MergeRequestId
import git.MergeRequestStatus
import models.ModuleCatalogGenerationRequest
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape

final class ModuleCatalogGenerationRequestTable(tag: Tag)
    extends Table[ModuleCatalogGenerationRequest](
      tag,
      "module_catalog_generation_request"
    ) {

  def mergeRequestId = column[MergeRequestId]("merge_request_id", O.PrimaryKey)

  def mergeRequestStatus = column[MergeRequestStatus]("merge_request_status")

  def semesterId = column[String]("semester", O.PrimaryKey)

  override def * : ProvenShape[ModuleCatalogGenerationRequest] = (
    mergeRequestId,
    semesterId,
    mergeRequestStatus
  ) <> (ModuleCatalogGenerationRequest.apply, ModuleCatalogGenerationRequest.unapply)
}
