package database.repo

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

trait Repository[Input, Output, T <: slick.jdbc.PostgresProfile.api.Table[Input]] {
  self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api.*

  protected implicit val ctx: ExecutionContext

  protected val tableQuery: TableQuery[T]

  protected def retrieve(
      query: Query[T, Input, Seq]
  ): Future[Seq[Output]]

  def all(): Future[Seq[Output]] =
    retrieve(tableQuery)

  def create(input: Input): Future[Input] =
    db.run(tableQuery.returning(tableQuery) += input)

  def createOrUpdate(l: Input): Future[Input] =
    db.run(createOrUpdateQuery(l))

  private def createOrUpdateQuery(l: Input) =
    tableQuery.returning(tableQuery).insertOrUpdate(l).map(_ => l)

  def createOrUpdateMany(ls: Seq[Input]): Future[Seq[Input]] =
    db.run(DBIO.sequence(ls.map(l => createOrUpdateQuery(l))))

  def createMany(ls: Seq[Input]): Future[Seq[Input]] =
    db.run(DBIO.sequence(ls.map(l => tableQuery.returning(tableQuery) += l)))
}
