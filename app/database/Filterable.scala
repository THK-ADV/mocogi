package database

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

private[database] trait Filterable[Input, T <: slick.jdbc.PostgresProfile.api.Table[Input]] {
  self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api.*

  private type Filter = Map[String, Seq[String]]
  private type Pred   = T => Rep[Boolean]

  protected val tableQuery: TableQuery[T]

  protected val makeFilter: PartialFunction[(String, String), Pred]

  private def parseFilter(filter: Filter): List[Pred] =
    filter.foldLeft(List.empty[Pred]) {
      case (xs, (key, values)) =>
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
}
