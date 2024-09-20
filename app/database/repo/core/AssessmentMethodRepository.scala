package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.Repository
import database.table.core.AssessmentMethodTable
import models.core.AssessmentMethod
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class AssessmentMethodRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[AssessmentMethod, AssessmentMethod, AssessmentMethodTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[AssessmentMethodTable]

  protected override def retrieve(
      query: Query[AssessmentMethodTable, AssessmentMethod, Seq]
  ) =
    db.run(query.result)

  def allIds() =
    db.run(tableQuery.map(_.id).result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)
}
