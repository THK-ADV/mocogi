package database

import com.github.tminglei.slickpg.*
import play.api.libs.json.JsValue
import play.api.libs.json.Json

trait MyPostgresProfile extends ExPostgresProfile, PgArraySupport, PgPlayJsonSupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  protected override def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends ExtPostgresAPI with ArrayImplicits with JsonImplicits {
    implicit val playJsonArrayTypeMapper: DriverJdbcType[List[JsValue]] =
      new AdvancedArrayJdbcType[JsValue](
        pgjson,
        s => utils.SimpleArrayUtils.fromString[JsValue](s => Json.parse(s))(s).orNull,
        v => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }
}

object MyPostgresProfile extends MyPostgresProfile
