package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.singleOpt
import database.repo.Repository
import database.table.core.SpecializationTable
import models.core.Specialization
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class SpecializationRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Specialization, Specialization, SpecializationTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[SpecializationTable]

  protected override def retrieve(
      query: Query[SpecializationTable, Specialization, Seq]
  ) =
    db.run(query.result)

  def allIds() =
    db.run(tableQuery.map(_.id).result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)

  def get(id: String): Future[Option[Specialization]] =
    db.run(tableQuery.filter(_.id === id).result.singleOpt)

  def allByPO(po: String): Future[Seq[Specialization]] =
    db.run(tableQuery.filter(_.po === po).result)
}
