package database.table

import slick.jdbc.PostgresProfile.api.*
import play.api.libs.json.JsValue

private[database] final class UserSettingsTable(tag: Tag)
    extends Table[(String, JsValue)](tag, None, "user_settings") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper

  def username = column[String]("username", O.PrimaryKey)

  def settings = column[JsValue]("settings")

  def * = (username, settings)
}