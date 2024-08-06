package database.repo.core

import database.repo.Repository
import database.table.core.GlobalCriteriaTable
import models.core.ModuleGlobalCriteria
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

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

  override protected def retrieve(
      query: Query[GlobalCriteriaTable, ModuleGlobalCriteria, Seq]
  ) =
    db.run(query.result)

  def allIds() =
    db.run(tableQuery.map(_.id).result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id inSet ids).delete)
}
