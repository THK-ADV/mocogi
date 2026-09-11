package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import database.table.core.ModuleTypeTable
import models.core.ModuleType
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class ModuleTypeRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[ModuleType, ModuleTypeTable] {
  import profile.api._

  protected val tableQuery = TableQuery[ModuleTypeTable]

  protected override def idOf(t: ModuleTypeTable) = t.id
}
