package database.repo

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import catalog.Semester
import database.table.ModuleCatalogGenerationRequestTable
import git.MergeRequestId
import git.MergeRequestStatus
import models.ModuleCatalogGenerationRequest
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleCatalogGenerationRequestRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      ModuleCatalogGenerationRequest,
      ModuleCatalogGenerationRequest,
      ModuleCatalogGenerationRequestTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.mergeRequestIdColumnType
  import database.table.mergeRequestStatusColumnType
  import profile.api._

  def exists(semester: Semester): Future[Boolean] =
    db.run(tableQuery.filter(_.semesterId === semester.id).exists.result)

  def get(mrId: MergeRequestId): Future[ModuleCatalogGenerationRequest] =
    db.run(tableQuery.filter(_.mergeRequestId === mrId).result.single)

  def get(
      mrId: MergeRequestId,
      semesterId: String
  ): Future[ModuleCatalogGenerationRequest] =
    db.run(
      tableQuery
        .filter(a => a.mergeRequestId === mrId && a.semesterId === semesterId)
        .result
        .single
    )

  def update(
      newStatus: MergeRequestStatus,
      existing: ModuleCatalogGenerationRequest
  ) =
    db.run(
      tableQuery
        .filter(a => a.mergeRequestId === existing.mergeRequestId && a.semesterId === existing.semesterId)
        .map(_.mergeRequestStatus)
        .update(newStatus)
    )

  def update(
      newMrId: MergeRequestId,
      newStatus: MergeRequestStatus,
      existing: ModuleCatalogGenerationRequest
  ) =
    db.run(
      tableQuery
        .filter(a => a.mergeRequestId === existing.mergeRequestId && a.semesterId === existing.semesterId)
        .map(a => (a.mergeRequestId, a.mergeRequestStatus))
        .update((newMrId, newStatus))
    )

  def delete(mrId: MergeRequestId) =
    db.run(tableQuery.filter(_.mergeRequestId === mrId).delete)

  def delete(mrId: MergeRequestId, semesterId: String) =
    db.run(
      tableQuery
        .filter(a => a.mergeRequestId === mrId && a.semesterId === semesterId)
        .delete
    )

  protected override val tableQuery: TableQuery[ModuleCatalogGenerationRequestTable] =
    TableQuery[ModuleCatalogGenerationRequestTable]

  protected override def retrieve(
      query: Query[
        ModuleCatalogGenerationRequestTable,
        ModuleCatalogGenerationRequest,
        Seq
      ]
  ): Future[Seq[ModuleCatalogGenerationRequest]] = db.run(query.result)
}
