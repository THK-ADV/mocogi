package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.Repository
import database.table.core.AssessmentMethodDbEntry
import database.table.core.AssessmentMethodTable
import models.core.AssessmentMethod
import models.AssessmentMethodSource
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class AssessmentMethodRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[AssessmentMethodDbEntry, AssessmentMethod, AssessmentMethodTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.given_BaseColumnType_AssessmentMethodSource
  import profile.api.*

  protected val tableQuery = TableQuery[AssessmentMethodTable]

  def allIds(): Future[Seq[String]] =
    db.run(tableQuery.map(_.id).result)

  def allBySource(source: AssessmentMethodSource): Future[Seq[AssessmentMethod]] =
    retrieve(tableQuery.filter(_.source === source))

  def deleteMany(ids: Seq[String]): Future[Int] =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)

  protected override def retrieve(
      query: Query[AssessmentMethodTable, AssessmentMethodDbEntry, Seq]
  ): Future[Seq[AssessmentMethod]] =
    db.run(query.result.map(_.map(a => AssessmentMethod(a.id, a.deLabel, a.enLabel))))
}
