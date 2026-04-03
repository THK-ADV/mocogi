package database.repo

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.libs.json.JsValue
import database.table.UserSettingsTable

@Singleton
final class UserSettingsRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  private val tableQuery = TableQuery[UserSettingsTable]

  def get(username: String): Future[Option[JsValue]] = {
    import database.MyPostgresProfile.MyAPI.playJsonTypeMapper
    db.run(tableQuery.filter(_.username === username).map(_.settings).result.map(_.headOption))
  }

  def update(username: String, settings: JsValue): Future[Unit] =
    db.run(tableQuery.insertOrUpdate((username, settings))).map(_ => ())
}
