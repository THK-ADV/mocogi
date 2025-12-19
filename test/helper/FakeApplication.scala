package helper

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.Configuration

trait FakeApplication {
  self: GuiceOneAppPerSuite =>

  protected def dbEnabled: Boolean = false

  def fakeConfig = {
    val config = if (dbEnabled) {
      Seq(
        "slick.dbs.default.db.url"             -> "jdbc:postgresql://localhost:5432/postgres",
        "slick.dbs.default.db.user"            -> "postgres",
        "slick.dbs.default.db.databaseName"    -> "postgres",
        "slick.dbs.default.db.password"        -> "",
        "play.evolutions.db.default.autoApply" -> "false",
        "play.evolutions.db.default.enabled"   -> "false"
      )
    } else {
      Seq(
        "slick.dbs.default.db.connectionPool"  -> "disabled",
        "play.evolutions.db.default.enabled"   -> "false",
        "play.evolutions.db.default.autoApply" -> "false"
      )
    }
    Configuration(config*)
  }

  implicit lazy val materializer: Materializer = app.materializer

  // import play.api.inject.bind
  protected def bindings: Seq[GuiceableModule] = Seq.empty

  override def fakeApplication() = new GuiceApplicationBuilder()
    .configure(fakeConfig)
    .overrides(bindings*)
    .build()
}
