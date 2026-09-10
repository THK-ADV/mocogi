package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import database.table.core.StatusTable
import models.core.ModuleStatus
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class StatusRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[ModuleStatus, StatusTable] {
  import profile.api._

  protected val tableQuery = TableQuery[StatusTable]

  protected override def idOf(t: StatusTable) = t.id
}
