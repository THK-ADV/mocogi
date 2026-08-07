package database

import java.util.UUID

import scala.concurrent.duration.*
import scala.concurrent.Await

import database.repo.ModuleDetailRepository
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.metadata.VersionScheme
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import service.moduledetails.ModuleDetails
import service.pipeline.MetadataPipeline
import service.ModuleService

final class ModuleDetailRepositorySpec extends AnyFunSuite with Matchers with GuiceOneAppPerSuite {

  private val moduleIds = List(
    "e37c5af9-6076-4f15-8c8b-d206b7091bc0",
    "8305a1c4-806b-47b9-a99f-e8cebea5211f",
    "696858c3-ce09-4dd7-8449-09bcd8a860a2",
    "6d7e31f7-0b9e-4162-be4e-89a977c0a9ed",
    "e3dc0278-cf5f-4296-a577-d88ad9c3e999",
    "05674322-071c-4a3a-8d8b-3c21c6bb640c"
  ).map(UUID.fromString)

  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "database.MyPostgresProfile$",
        "slick.dbs.default.db.url"  -> sys.env.getOrElse(
          "TEST_JDBC_URL",
          "jdbc:postgresql://localhost:5432/mocogi_test"
        ),
        "slick.dbs.default.db.user"                 -> sys.env.getOrElse("TEST_DB_USER", "postgres"),
        "slick.dbs.default.db.password"             -> sys.env.getOrElse("TEST_DB_PASSWORD", ""),
        "play.evolutions.db.default.enabled"        -> false,
        "play.evolutions.db.default.autoApply"      -> false,
        "play.evolutions.db.default.autoApplyDowns" -> false
      )
      .build()

  test("custom assembly matches get_module_details semantics") {
    val moduleService = app.injector.instanceOf[ModuleService]
    val pipeline      = app.injector.instanceOf[MetadataPipeline]
    val repository    = app.injector.instanceOf[ModuleDetailRepository]

    moduleIds.foreach { moduleId =>
      val protocol = await(moduleService.get(moduleId))
      val module   = await(pipeline.printParseValidate(protocol, VersionScheme.default, moduleId))
        .fold(error => fail(error.toString), _._1)
      val expected = await(repository.getModuleDetails(moduleId)).flatMap(_.toOption).getOrElse(fail("missing details"))
      val actual   = await(repository.assemble(module, expected.lastModified))

      withClue(s"module $moduleId\n")(normalize(actual) shouldEqual normalize(expected))
    }
  }

  private def normalize(details: ModuleDetails): JsObject = {
    val json            = Json.toJson(details).as[JsObject]
    val unorderedFields =
      Set("examPhases", "moduleManagement", "lecturer", "assessments", "poMandatory", "poOptional", "taughtWith")
    val prerequisites = Set("recommendedPrerequisites", "requiredPrerequisites")

    json ++ JsObject(unorderedFields.toSeq.map(field => field -> sorted(json(field)))) ++ JsObject(
      prerequisites.toSeq.map { field =>
        field -> (json(field) match {
          case prerequisite: JsObject => prerequisite + ("modules" -> sorted(prerequisite("modules")))
          case value                  => value
        })
      }
    )
  }

  private def sorted(value: JsValue): JsValue = value match {
    case JsArray(values) => JsArray(values.sortBy(Json.stringify))
    case value           => value
  }

  private def await[A](future: scala.concurrent.Future[A]): A =
    Await.result(future, 120.seconds)
}
