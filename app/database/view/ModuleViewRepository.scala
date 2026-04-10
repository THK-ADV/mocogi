package database.view

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.Schema
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleViewRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with MaterializedView {
  import profile.api.*

  override def name: String = "module_view"

  override def schema = Schema.Modules.name

  def all(): Future[String] =
    db.run(sql"""SELECT coalesce(jsonb_agg(to_jsonb(m)), '[]'::jsonb) FROM modules.module_view m""".as[String].head)
}
