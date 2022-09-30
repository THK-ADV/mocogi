package database.repo

import basedata.Person
import database.table.PersonTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PersonRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[Person, PersonTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[PersonTable]
}
