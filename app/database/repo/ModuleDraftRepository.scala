package database.repo

import database.table.ModuleDraftTable
import database.{Filterable, table}
import models.ModuleDraft
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
import service.Print
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleDraftRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleDraft, ModuleDraft, ModuleDraftTable]
    with HasDatabaseConfigProvider[JdbcProfile]
    /*with Filterable[ModuleDraft, ModuleDraftTable]*/ {
  import profile.api._
  import table.{jsValueColumnType, printColumnType}

  protected val tableQuery = TableQuery[ModuleDraftTable]

  override protected def retrieve(
      query: Query[ModuleDraftTable, ModuleDraft, Seq]
  ) =
    db.run(query.result)

//  override protected val makeFilter = { case ("branch", branch) =>
//    _.branch.toLowerCase === branch.toLowerCase
//  }

//  def allFromBranch(branch: String) =
//    retrieve(allWithFilter(Map(("branch", Seq(branch)))))
//
//  def get(module: UUID): Future[Option[ModuleDraft]] =
//    db.run(
//      tableQuery.filter(_.module === module).take(1).result.map(_.headOption)
//    )
//
//  def delete(branch: String) =
//    db.run(tableQuery.filter(_.branch === branch).delete)
//
//  def update(draft: ModuleDraft) =
//    db.run(
//      tableQuery
//        .filter(_.module === draft.module)
//        .update(draft)
//        .map(_ => draft)
//    )
//
//  def updateValidation(
//      id: UUID,
//      e: Either[JsValue, (JsValue, Print)]
//  ): Future[Int] = {
//    def go: (Option[JsValue], Option[Print], Option[JsValue]) = e match {
//      case Left(err)            => (None, None, Some(err))
//      case Right((json, print)) => (Some(json), Some(print), None)
//    }
//    db.run(
//      tableQuery
//        .filter(_.module === id)
//        .map(a =>
//          (a.moduleCompendium, a.moduleCompendiumPrint, a.pipelineErrorJson)
//        )
//        .update(go)
//    )
//  }
}
