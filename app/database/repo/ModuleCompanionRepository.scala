package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.ModuleCompanionTable
import models.ModuleCompanion
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleCompanionRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api.*

  private val tableQuery = TableQuery[ModuleCompanionTable]

  def createMany(xs: Seq[ModuleCompanion]): Future[Unit] =
    db.run(tableQuery ++= xs).map(_ => ())

  def allFromModules(modules: Seq[UUID]): Future[Seq[ModuleCompanion]] =
    db.run(tableQuery.filter(_.module.inSet(modules)).result)
}
