package database.repo.core

import database.repo.Repository
import database.table.core.LanguageTable
import models.core.ModuleLanguage
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class LanguageRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleLanguage, ModuleLanguage, LanguageTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[LanguageTable]

  override protected def retrieve(query: Query[LanguageTable, ModuleLanguage, Seq]) =
    db.run(query.result)
}
