package helper

import database.table.ModuleDraftTable
import org.apache.pekko.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.Configuration
import slick.dbio.DBIOAction
import slick.dbio.Effect
import slick.dbio.NoStream
import slick.lifted.TableQuery

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

  def withFreshDb(actions: DBIOAction[?, NoStream, Effect.All]*) = {
    import slick.jdbc.PostgresProfile.api._
    val db = app.injector.instanceOf(classOf[DatabaseConfigProvider]).get.db

    db.run(
      DBIO.seq(
        sqlu"drop schema public cascade",
        sqlu"create schema public",
        moduleDraftTable.schema.create,
        DBIO.seq(actions*)
      )
    )
  }

  val moduleDraftTable = TableQuery[ModuleDraftTable]
}
