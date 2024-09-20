package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.Repository
import database.table.core.LanguageTable
import models.core.ModuleLanguage
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class LanguageRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[ModuleLanguage, ModuleLanguage, LanguageTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[LanguageTable]

  protected override def retrieve(
      query: Query[LanguageTable, ModuleLanguage, Seq]
  ) =
    db.run(query.result)

  def allIds() =
    db.run(tableQuery.map(_.id).result)

  def deleteMany(ids: Seq[String]) =
    db.run(tableQuery.filter(_.id.inSet(ids)).delete)
}
