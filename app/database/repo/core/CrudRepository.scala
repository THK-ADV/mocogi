package database.repo.core

import scala.concurrent.Future

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

trait CrudRepository[A] {
  def list(): Future[Seq[A]]
  def create(input: A): Future[A]
  def update(id: String, input: A): Future[Int]
}

/** Default read, create and update operations for resources stored in a single table. */
private[repo] trait TableCrudRepository[A, T <: slick.jdbc.PostgresProfile.api.Table[A]] extends CrudRepository[A] {
  self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api.*

  protected val tableQuery: TableQuery[T]

  protected def idOf(t: T): Rep[String]

  def list(): Future[Seq[A]] =
    db.run(tableQuery.result)

  def create(input: A): Future[A] =
    db.run(tableQuery.returning(tableQuery) += input)

  def update(id: String, a: A): Future[Int] =
    db.run(tableQuery.filter(idOf(_) === id).update(a))
}
