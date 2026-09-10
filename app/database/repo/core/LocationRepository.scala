package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import database.table.core.LocationTable
import models.core.ModuleLocation
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class LocationRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[ModuleLocation, LocationTable] {
  import profile.api._

  protected val tableQuery = TableQuery[LocationTable]

  protected override def idOf(t: LocationTable) = t.id
}
