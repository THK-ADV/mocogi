package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import database.table.core.SeasonTable
import models.core.Season
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class SeasonRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[Season, SeasonTable] {
  import profile.api._

  protected val tableQuery = TableQuery[SeasonTable]

  protected override def idOf(t: SeasonTable) = t.id
}
