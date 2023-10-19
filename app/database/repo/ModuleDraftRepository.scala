package database.repo

import database.table
import database.table.{ModuleDraftTable, branchColumnType}
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

  def allByModules(modules: Seq[UUID]): Future[Seq[ModuleDraft]]

  def allByUser(user: User): Future[Seq[ModuleDraft]]

  def updateDraft(
      moduleId: UUID,
      data: JsValue,
      mc: JsValue,
      print: Print,
      keysToBeReviewed: Set[String],
      modifiedKeys: Set[String],
      lastCommit: CommitId
  ): Future[Int]

  def delete(moduleId: UUID): Future[Int]

  def deleteDrafts(moduleIds: Seq[UUID]): Future[Int]

  def getByModule(moduleId: UUID): Future[ModuleDraft]

  def getByModuleOpt(moduleId: UUID): Future[Option[ModuleDraft]]

  def hasModuleDraft(moduleId: UUID): Future[Boolean]

  def updateMergeRequestId(
      moduleId: UUID,
      mergeRequestId: Option[MergeRequestId]
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
    mergeRequestColumnType,
    printColumnType,
    setStringColumnType,
    userColumnType
  }

  protected val tableQuery = TableQuery[ModuleDraftTable]

  override protected def retrieve(
      query: Query[ModuleDraftTable, ModuleDraft, Seq]
  ) =
    db.run(query.result)

  def allByModules(modules: Seq[UUID]): Future[Seq[ModuleDraft]] =
    db.run(tableQuery.filter(_.module.inSet(modules)).result)

  override def allByUser(user: User): Future[Seq[ModuleDraft]] =
    db.run(tableQuery.filter(_.user === user).result)

  def delete(moduleId: UUID): Future[Int] =
    db.run(tableQuery.filter(_.module === moduleId).delete)

  override def deleteDrafts(moduleIds: Seq[UUID]) =
    db.run(tableQuery.filter(_.module.inSet(moduleIds)).delete)

  override def hasModuleDraft(moduleId: UUID) =
    db.run(tableQuery.filter(_.module === moduleId).exists.result)

  override def updateMergeRequestId(
      moduleId: UUID,
      mergeRequestId: Option[MergeRequestId]
  ) =
    db.run(
      tableQuery
        .filter(_.module === moduleId)
        .map(_.mergeRequestId)
        .update(mergeRequestId)
    ).map(_ => ())

  def updateDraft(
      moduleId: UUID,
      data: JsValue,
      mc: JsValue,
      print: Print,
      keysToBeReviewed: Set[String],
      modifiedKeys: Set[String],
      lastCommit: CommitId
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(_.module === moduleId)
        .map(a =>
          (
            a.data,
            a.moduleCompendium,
            a.moduleCompendiumPrint,
            a.keysToBeReviewed,
            a.modifiedKeys,
            a.lastCommit,
            a.lastModified
          )
        )
        .update(
          (
            data,
            mc,
            print,
            keysToBeReviewed,
            modifiedKeys,
            Some(lastCommit),
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
}
