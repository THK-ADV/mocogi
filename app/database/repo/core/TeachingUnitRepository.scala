package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import database.table.core.TeachingUnitTable
import models.core.TeachingUnit
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class TeachingUnitRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[TeachingUnit, TeachingUnitTable] {
  import profile.api.*

  protected val tableQuery = TableQuery[TeachingUnitTable]

  protected override def idOf(t: TeachingUnitTable) = t.id.asColumnOf[String]
}
