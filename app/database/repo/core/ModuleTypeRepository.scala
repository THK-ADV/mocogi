package database.repo.core

import database.repo.Repository
import database.table.core.ModuleTypeTable
import models.core.ModuleType
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ModuleTypeRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleType, ModuleType, ModuleTypeTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[ModuleTypeTable]

  override protected def retrieve(
      query: Query[ModuleTypeTable, ModuleType, Seq]
  ) =
    db.run(query.result)
}
