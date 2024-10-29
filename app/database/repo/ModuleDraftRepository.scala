package database.repo

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table
import database.table.ModuleDraftTable
import git.CommitId
import git.MergeRequestId
import git.MergeRequestStatus
import models.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.json.JsValue
import service.Print
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleDraftRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleDraft, ModuleDraft, ModuleDraftTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*
  import table.commitColumnType
  import table.jsValueColumnType
  import table.mergeRequestIdColumnType
  import table.mergeRequestStatusColumnType
  import table.printColumnType
  import table.setStringColumnType

  protected val tableQuery = TableQuery[ModuleDraftTable]

  protected override def retrieve(
      query: Query[ModuleDraftTable, ModuleDraft, Seq]
  ) =
    db.run(query.result)

  def getMergeRequestId(module: UUID): Future[Option[MergeRequestId]] =
    db.run(tableQuery.filter(_.module === module).map(_.mergeRequestId).result.single)

  def delete(moduleId: UUID): Future[Int] =
    db.run(tableQuery.filter(_.module === moduleId).delete)

  def hasModuleDraft(moduleId: UUID) =
    db.run(tableQuery.filter(_.module === moduleId).exists.result)

  def updateMergeRequestStatus(
      moduleId: UUID,
      status: MergeRequestStatus
  ) =
    db.run(
      tableQuery
        .filter(_.module === moduleId)
        .map(_.mergeRequestStatus)
        .update(Some(status))
        .map(_ => ())
    )

  def updateMergeRequest(
      moduleId: UUID,
      mergeRequest: Option[(MergeRequestId, MergeRequestStatus)]
  ) =
    db.run(
      tableQuery
        .filter(_.module === moduleId)
        .map(a => (a.mergeRequestId, a.mergeRequestStatus))
        .update(mergeRequest.map(_._1), mergeRequest.map(_._2))
        .map(_ => ())
    )

  def updateDraft(
      moduleId: UUID,
      moduleTitle: String,
      moduleAbbrev: String,
      data: JsValue,
      module: JsValue,
      print: Print,
      keysToBeReviewed: Set[String],
      modifiedKeys: Set[String],
      lastCommit: CommitId,
      mergeRequest: Option[(MergeRequestId, MergeRequestStatus)]
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(_.module === moduleId)
        .map(a =>
          (
            a.moduleTitle,
            a.moduleAbbrev,
            a.data,
            a.moduleValidated,
            a.modulePrint,
            a.keysToBeReviewed,
            a.modifiedKeys,
            a.lastCommit,
            a.mergeRequestId,
            a.mergeRequestStatus,
            a.lastModified
          )
        )
        .update(
          (
            moduleTitle,
            moduleAbbrev,
            data,
            module,
            print,
            keysToBeReviewed,
            modifiedKeys,
            Some(lastCommit),
            mergeRequest.map(_._1),
            mergeRequest.map(_._2),
            LocalDateTime.now
          )
        )
    )

  def getByModule(moduleId: UUID): Future[ModuleDraft] =
    db.run(
      tableQuery
        .filter(_.module === moduleId)
        .take(1)
        .result
        .flatMap(
          _.headOption
            .map(DBIO.successful)
            .getOrElse(
              DBIOAction.failed(
                new Throwable(s"module draft for module $moduleId not found")
              )
            )
        )
    )

  def getByModuleOpt(moduleId: UUID) =
    db.run(tableQuery.filter(_.module === moduleId).result.map(_.headOption))

  def isAuthorOf(moduleId: UUID, personId: String) =
    db.run(
      tableQuery
        .filter(a => a.module === moduleId && a.author === personId)
        .exists
        .result
    )
}
