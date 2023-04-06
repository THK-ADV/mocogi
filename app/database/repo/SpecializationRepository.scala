package database.repo

import database.table.SpecializationTable
import models.core.Specialization
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class SpecializationRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Specialization, Specialization, SpecializationTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[SpecializationTable]

  // TODO add this to repository
  def exists(abbrev: String): Future[Boolean] =
    db.run(tableQuery.filter(_.abbrev === abbrev).exists.result)

  override protected def retrieve(
      query: Query[SpecializationTable, Specialization, Seq]
  ) =
    db.run(query.result)

  def update(s: Specialization) =
    db.run(tableQuery.filter(_.abbrev === s.abbrev).update(s).map(_ => s))
}
