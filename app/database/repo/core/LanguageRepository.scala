package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import database.table.core.LanguageTable
import models.core.ModuleLanguage
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class LanguageRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
    with TableCrudRepository[ModuleLanguage, LanguageTable] {
  import profile.api._

  protected val tableQuery = TableQuery[LanguageTable]

  protected override def idOf(t: LanguageTable) = t.id
}
