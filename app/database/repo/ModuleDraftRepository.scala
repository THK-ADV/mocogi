package database.repo

import database.Filterable
import database.table.ModuleDraftTable
import models.ModuleDraft
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
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
    with Filterable[ModuleDraft, ModuleDraftTable] {
  import profile.api._

  type McJson = String
  type McPrint = String
  type Errors = String

  protected val tableQuery = TableQuery[ModuleDraftTable]

  override protected def retrieve(
      query: Query[ModuleDraftTable, ModuleDraft, Seq]
  ) =
    db.run(query.result)

  override protected val makeFilter = { case ("branch", branch) =>
    _.branch.toLowerCase === branch.toLowerCase
  }

  def allFromBranch(branch: String) =
    retrieve(allWithFilter(Map(("branch", Seq(branch)))))

  def get(module: UUID): Future[Option[ModuleDraft]] =
    db.run(
      tableQuery.filter(_.module === module).take(1).result.map(_.headOption)
    )

  def update(draft: ModuleDraft) =
    db.run(
      tableQuery
        .filter(_.module === draft.module)
        .update(draft)
        .map(_ => draft)
    )

  def updateValidation(id: UUID, e: Either[Errors, (McJson, McPrint)]) = {
    def go = e match {
      case Left(err)            => (None, None, Some(err))
      case Right((json, print)) => (Some(json), Some(print), None)
    }
    db.run(
      tableQuery
        .filter(_.module === id)
        .map(a =>
          (a.moduleCompendiumJson, a.moduleCompendiumPrint, a.pipelineError)
        )
        .update(go)
    )
  }
}
