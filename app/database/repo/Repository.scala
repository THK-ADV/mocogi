package database.repo

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

trait Repository[A, T <: Table[A]] {
  self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  protected val tableQuery: TableQuery[T]

  def all(): Future[Seq[A]] =
    db.run(tableQuery.result)

  def createOrUpdate(l: A): Future[Option[A]] =
    db.run((tableQuery returning tableQuery).insertOrUpdate(l))

  def createMany(ls: List[A]): Future[List[A]] =
    db.run(DBIO.sequence(ls.map(l => tableQuery returning tableQuery += l)))
}
