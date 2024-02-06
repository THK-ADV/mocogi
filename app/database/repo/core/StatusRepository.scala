package database.repo.core

import database.repo.Repository
import database.table.core.StatusTable
import models.core.Status
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class StatusRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Status, Status, StatusTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[StatusTable]

  override protected def retrieve(query: Query[StatusTable, Status, Seq]) =
    db.run(query.result)
}
