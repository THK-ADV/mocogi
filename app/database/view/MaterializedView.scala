package database.view

import scala.concurrent.Future

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

private[view] trait MaterializedView { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  def name: String

  def refreshView(): Future[Int] =
    db.run(sqlu"refresh materialized view #$name")
}
