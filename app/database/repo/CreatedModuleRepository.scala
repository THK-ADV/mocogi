package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.CreatedModuleTable
import models.CreatedModule
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class CreatedModuleRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api.*

  private val tableQuery = TableQuery[CreatedModuleTable]

  def create(module: CreatedModule): Future[Unit] =
    db.run(tableQuery.insertOrUpdate(module)).map(_ => ())

  def delete(modules: Seq[UUID]): Future[Int] =
    db.run(tableQuery.filter(_.module.inSet(modules)).delete)
}
