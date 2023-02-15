package database.repo

import database.table.FacultyTable
import models.core.Faculty
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class FacultyRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Faculty, Faculty, FacultyTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  protected val tableQuery = TableQuery[FacultyTable]

  override protected def retrieve(query: Query[FacultyTable, Faculty, Seq]) =
    db.run(query.result)
}
