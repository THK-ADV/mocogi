package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import database.table.core.DegreeTable
import models.core.Degree
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class DegreeRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[Degree, DegreeTable] {
  import profile.api._
  protected val tableQuery = TableQuery[DegreeTable]

  protected override def idOf(t: DegreeTable) = t.id
}
