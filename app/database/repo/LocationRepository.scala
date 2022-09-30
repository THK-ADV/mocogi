package database.repo

import basedata.Location
import database.table.LocationTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class LocationRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Location, LocationTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[LocationTable]
}
