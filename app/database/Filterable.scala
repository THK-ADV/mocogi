package database

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api.Table

import scala.annotation.unused
import scala.concurrent.Future

trait Filterable[Input, T <: Table[Input]] {
  self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api.*

  type Filter = Map[String, Seq[String]]
  type Pred = T => Rep[Boolean]

  protected val tableQuery: TableQuery[T]

  protected val makeFilter: PartialFunction[(String, String), Pred]

  private def parseFilter(filter: Filter): List[Pred] =
    filter.foldLeft(List.empty[Pred]) { case (xs, (key, values)) =>
      if (values.nonEmpty && makeFilter.isDefinedAt((key, values.head)))
        makeFilter.apply((key, values.head)) :: xs
      else
        xs
    }

  private def combinePreds(xs: List[Pred]): Pred =
    xs.reduceLeftOption[Pred]((lhs, rhs) => t => lhs(t) && rhs(t))
      .getOrElse(_ => true)

  final def allWithFilter(filter: Filter): Query[T, Input, Seq] =
    tableQuery.filter(combinePreds(parseFilter(filter)))

  @unused
  final def getAllWithFilter(filter: Filter): Future[Seq[Input]] =
    db.run(allWithFilter(filter).result)

  @unused
  def asBoolean(value: String, p: Boolean => Rep[Boolean]): Rep[Boolean] =
    value.toBooleanOption.map(p).getOrElse(false)
}
