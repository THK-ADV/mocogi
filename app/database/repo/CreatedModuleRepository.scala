package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.CreatedModuleTable
import models.CreatedModule
import models.ModuleCore
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

  def allAsModuleCore(): Future[Seq[ModuleCore]] =
    db.run(tableQuery.map(a => (a.module, a.moduleTitle, a.moduleAbbrev)).result.map(_.map(ModuleCore.apply.tupled)))

  def allGenericWithPOsAsModuleCore(): Future[Seq[(ModuleCore, Seq[String])]] = {
    import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
    db.run(
      tableQuery
        .filter(_.moduleType === "generic_module")
        .map(m => ((m.module, m.moduleTitle, m.moduleAbbrev), m.moduleMandatoryPOs))
        .result
        .map(_.map(m => (ModuleCore(m._1._1, m._1._2, m._1._3), m._2.toSeq)))
    )
  }
}
