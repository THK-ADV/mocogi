package database.repo.core

import database.repo.Repository
import database.table.core.CompetenceTable
import models.core.Competence
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CompetenceRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Competence, Competence, CompetenceTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  protected val tableQuery = TableQuery[CompetenceTable]

  override protected def retrieve(
      query: Query[CompetenceTable, Competence, Seq]
  ) =
    db.run(query.result)
}
