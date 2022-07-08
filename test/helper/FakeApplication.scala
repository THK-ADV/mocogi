package helper

import akka.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}

trait FakeApplication {
  self: GuiceOneAppPerSuite =>

  val fakeConfig = Configuration(
    "filePaths.seasons" -> "test/parsing/res/season.yaml",
    "filePaths.persons" -> "test/parsing/res/person.yaml",
    "filePaths.status" -> "test/parsing/res/status.yaml",
    "filePaths.moduleTypes" -> "test/parsing/res/module_type.yaml",
    "filePaths.locations" -> "test/parsing/res/location.yaml",
    "filePaths.languages" -> "test/parsing/res/lang.yaml",
    "filePaths.assessmentMethods" -> "test/parsing/res/assessment.yaml",
    "slick.dbs.default.db.connectionPool" -> "disabled",
    "play.evolutions.db.default.autoApply" -> "false"
  )

  implicit lazy val materializer: Materializer = app.materializer

  // import play.api.inject.bind
  protected def bindings: Seq[GuiceableModule] = Seq.empty

  override def fakeApplication() = new GuiceApplicationBuilder()
    .configure(fakeConfig)
    .overrides(bindings: _*)
    .build()
}
