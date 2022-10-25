package database.repo

import basedata.Person
import database.entities.PersonDbEntry
import database.table.PersonTable
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PersonRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[PersonDbEntry, Person, PersonTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._
  protected val tableQuery = TableQuery[PersonTable]

  override protected def retrieve(
      query: Query[PersonTable, PersonDbEntry, Seq]
  ) =
    db.run(
      (
        for {
          q <- query
          f <- q.facultyFk
        } yield (q, f)
      ).result.map(_.map(makePerson))
    )
}
