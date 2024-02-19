package database.repo.core

import database.repo.Repository
import database.table.core.SpecializationTable
import models.core.Specialization
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class SpecializationRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Specialization, Specialization, SpecializationTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[SpecializationTable]

  override protected def retrieve(
      query: Query[SpecializationTable, Specialization, Seq]
  ) =
    db.run(query.result)
}
