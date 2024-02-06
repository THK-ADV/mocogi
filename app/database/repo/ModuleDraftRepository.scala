package database.repo

import database.table
import database.table.ModuleDraftTable
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
import service.Print
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ModuleDraftRepository {
  def create(moduleDraft: ModuleDraft): Future[ModuleDraft]

  def isAuthorOf(moduleId: UUID, personId: String): Future[Boolean]

  def allByAuthor(personId: String): Future[Seq[ModuleDraft]]

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
  ): Future[Int]

  def delete(moduleId: UUID): Future[Int]

  def deleteDrafts(moduleIds: Seq[UUID]): Future[Int]

  def getByModule(moduleId: UUID): Future[ModuleDraft]

  def getByModuleOpt(moduleId: UUID): Future[Option[ModuleDraft]]

  def hasModuleDraft(moduleId: UUID): Future[Boolean]

  def updateMergeRequestStatus(
      moduleId: UUID,
      status: MergeRequestStatus
  ): Future[Unit]

  def updateMergeRequest(
      moduleId: UUID,
      mergeRequest: Option[(MergeRequestId, MergeRequestStatus)]
  ): Future[Unit]
}

@Singleton
final class ModuleDraftRepositoryImpl @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends ModuleDraftRepository
    with Repository[ModuleDraft, ModuleDraft, ModuleDraftTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  import table.{
    commitColumnType,
    jsValueColumnType,
    mergeRequestIdColumnType,
    mergeRequestStatusColumnType,
    printColumnType,
    setStringColumnType
  }

  protected val tableQuery = TableQuery[ModuleDraftTable]

  override protected def retrieve(
      query: Query[ModuleDraftTable, ModuleDraft, Seq]
  ) =
    db.run(query.result)

  override def allByAuthor(personId: String): Future[Seq[ModuleDraft]] =
    db.run(tableQuery.filter(_.author === personId).result)

  def delete(moduleId: UUID): Future[Int] =
    db.run(tableQuery.filter(_.module === moduleId).delete)

  override def deleteDrafts(moduleIds: Seq[UUID]) =
    db.run(tableQuery.filter(_.module.inSet(moduleIds)).delete)

  override def hasModuleDraft(moduleId: UUID) =
    db.run(tableQuery.filter(_.module === moduleId).exists.result)

  override def updateMergeRequestStatus(
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

  override def updateMergeRequest(
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
          _.headOption.map(DBIO.successful) getOrElse DBIOAction.failed(
            new Throwable(s"module draft for module $moduleId not found")
          )
        )
    )

  override def getByModuleOpt(moduleId: UUID) =
    db.run(tableQuery.filter(_.module === moduleId).result.map(_.headOption))

  override def isAuthorOf(moduleId: UUID, personId: String) =
    db.run(
      tableQuery
        .filter(a => a.module === moduleId && a.author === personId)
        .exists
        .result
    )
}
