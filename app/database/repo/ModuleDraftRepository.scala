package database.repo

import database.Filterable
import database.table.ModuleDraftTable
import models.ModuleDraft
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleDraftRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleDraft, ModuleDraft, ModuleDraftTable]
    with HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[ModuleDraft, ModuleDraftTable] {
  import profile.api._

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
}