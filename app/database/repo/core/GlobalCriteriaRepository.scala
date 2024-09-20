package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.Repository
import database.table.core.GlobalCriteriaTable
import models.core.ModuleGlobalCriteria
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class GlobalCriteriaRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      ModuleGlobalCriteria,
      ModuleGlobalCriteria,
      GlobalCriteriaTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  protected val tableQuery = TableQuery[GlobalCriteriaTable]

  protected override def retrieve(
      query: Query[GlobalCriteriaTable, ModuleGlobalCriteria, Seq]
  ) =
    db.run(query.result)

  def allIds() =
    db.run(tableQuery.map(_.id).result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)
}
