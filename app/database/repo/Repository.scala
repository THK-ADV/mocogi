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

  def create(input: Input): Future[Input] =
    db.run(tableQuery returning tableQuery += input)

  def createOrUpdate(l: Input): Future[Input] =
    db.run(createOrUpdateQuery(l))

  private def createOrUpdateQuery(l: Input) =
    (tableQuery returning tableQuery).insertOrUpdate(l).map(_ => l)

  def createOrUpdateMany(
      ls: Seq[Input]
  ): Future[Seq[Input]] =
    db.run(DBIO.sequence(ls.map(l => createOrUpdateQuery(l))))

  def createMany(ls: Seq[Input]): Future[Seq[Input]] =
    db.run(DBIO.sequence(ls.map(l => tableQuery returning tableQuery += l)))
}
