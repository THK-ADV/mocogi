package database.repo.core

import database.repo.Repository
import database.table.core.LocationTable
import models.core.Location
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class LocationRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Location, Location, LocationTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[LocationTable]

  override protected def retrieve(query: Query[LocationTable, Location, Seq]) =
    db.run(query.result)
}
