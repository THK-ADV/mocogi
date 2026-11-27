package database.table

import database.Schema
import play.api.libs.json.JsValue
import slick.jdbc.PostgresProfile.api.*

private[database] final class ExamListConfigTable(tag: Tag)
    extends Table[(String, JsValue)](tag, Some(Schema.Exams.name), "exam_list_config") {

  import database.MyPostgresProfile.MyAPI.playJsonTypeMapper

  def po = column[String]("po", O.PrimaryKey)

  def config = column[JsValue]("config")

  override def * = (po, config)
}
