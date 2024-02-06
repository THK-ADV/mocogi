package database.repo.core

import database.repo.Repository
import database.table.core.DegreeTable
import models.core.Degree
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DegreeRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Degree, Degree, DegreeTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  protected val tableQuery = TableQuery[DegreeTable]

  override protected def retrieve(query: Query[DegreeTable, Degree, Seq]) =
    db.run(query.result)
}
