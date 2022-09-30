package database.repo

import basedata.Language
import database.table.LanguageTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class LanguageRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Language, LanguageTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[LanguageTable]
}
