package database.repo.schedule

import javax.inject.Inject
import javax.inject.Singleton

import database.repo.core.TableCrudRepository
import database.table.schedule.RoomTable
import models.schedule.Room
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class RoomRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[Room, RoomTable] {
  import profile.api.*

  protected val tableQuery = TableQuery[RoomTable]

  protected override def idOf(t: RoomTable) = t.id.asColumnOf[String]
}
