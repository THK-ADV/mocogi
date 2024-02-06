package database.repo.core

import database.repo.Repository
import database.table.core.FocusAreaTable
import models.core.FocusArea
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class FocusAreaRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[FocusArea, FocusArea, FocusAreaTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[FocusAreaTable]

  override protected def retrieve(
      query: Query[FocusAreaTable, FocusArea, Seq]
  ) =
    db.run(query.result)
}
