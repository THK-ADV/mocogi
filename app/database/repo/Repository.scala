package database.repo

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait Repository[Input, Output, T <: Table[Input]] {
  self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  protected implicit val ctx: ExecutionContext

  protected val tableQuery: TableQuery[T]

  protected def retrieve(
      query: Query[T, T#TableElementType, Seq]
  ): Future[Seq[Output]]

  def all(): Future[Seq[Output]] =
    retrieve(tableQuery)

  def createOrUpdate(l: Input): Future[Option[Input]] =
    db.run((tableQuery returning tableQuery).insertOrUpdate(l))

  def createMany(ls: List[Input]): Future[List[Input]] =
    db.run(DBIO.sequence(ls.map(l => tableQuery returning tableQuery += l)))
}
