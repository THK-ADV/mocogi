package database.repo.core

import database.repo.Repository
import database.table.core.AssessmentMethodTable
import models.core.AssessmentMethod
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AssessmentMethodRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[AssessmentMethod, AssessmentMethod, AssessmentMethodTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[AssessmentMethodTable]

  override protected def retrieve(
      query: Query[AssessmentMethodTable, AssessmentMethod, Seq]
  ) =
    db.run(query.result)
}
