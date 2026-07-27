package database

import java.util.UUID

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.ExecutionContext

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import database.repo.ModuleRepository
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.BasicProfile
import slick.basic.DatabaseConfig

final class ModuleRepositorySpec extends AnyFunSuite with Matchers with OptionValues with BeforeAndAfterAll {

  private given ExecutionContext = ExecutionContext.global

  private val parentId = UUID.fromString("e3dc0278-cf5f-4296-a577-d88ad9c3e999")
  private val childIds = Set(
    UUID.fromString("05674322-071c-4a3a-8d8b-3c21c6bb640c"),
    UUID.fromString("23ce931b-4d27-47da-9158-3ab8979759cf")
  )

  private lazy val repository = ModuleRepository(databaseConfigProvider, summon[ExecutionContext])

  override def beforeAll(): Unit =
    TestDb.start()

  test("retrieve assembles module dependencies without duplicates") {
    val result = await(repository.all(Map("id" -> Seq(parentId.toString))))

    result should have size 1
    val module = result.head._1
    module.id.value shouldBe parentId
    module.metadata.moduleRelation.value.children.toList.toSet shouldBe childIds
    module.metadata.moduleManagement.toList.toSet shouldBe Set("jos")
    module.metadata.lecturers.toList.toSet shouldBe Set("rotating")
    module.metadata.po.mandatory.map(_.po).toSet shouldBe Set("inf_inf2", "inf_itm2", "inf_mi4", "inf_wi5")
  }

  test("retrieve resolves taught with to the counterpart module") {
    import MyPostgresProfile.api.*
    val relations = TestDb.runSync(TableQuery[table.ModuleTaughtWithTable].result)
    assume(relations.nonEmpty, "expected taught with relations in the test database")

    val relation = relations.head
    val module   = await(repository.all(Map("id" -> Seq(relation.module.toString)))).head._1

    module.metadata.taughtWith should contain(relation.moduleTaught)
    module.metadata.taughtWith should not contain relation.module
  }

  test("retrieve returns an empty result for an unknown module") {
    val result = await(repository.all(Map("id" -> Seq(UUID.randomUUID().toString))))

    result shouldBe empty
  }

  test("allModuleCoreWithRelations returns every core once and indexes children by parent") {
    val (modules, relations) = await(repository.allModuleCoreWithRelations())

    modules.map(_.id).distinct should have size modules.size
    relations(parentId) shouldBe childIds
  }

  test("allFromPO includes children inherited from a matching parent") {
    val modules = await(repository.allFromPO("inf_inf2", activeOnly = true)).map(_._1)
    val ids     = modules.flatMap(_.id).toSet

    ids should contain(parentId)
    childIds.subsetOf(ids) shouldBe true
  }

  private def await[A](future: scala.concurrent.Future[A]): A =
    Await.result(future, 30.seconds)

  private lazy val databaseConfigProvider = new DatabaseConfigProvider {
    override def get[P <: BasicProfile]: DatabaseConfig[P] =
      databaseConfig.asInstanceOf[DatabaseConfig[P]]
  }

  private lazy val databaseConfig = new DatabaseConfig[MyPostgresProfile.type] {
    override val profile: MyPostgresProfile.type = MyPostgresProfile
    override val db: profile.backend.Database    = TestDb.db
    override val config: Config                  = ConfigFactory.empty()
    override val profileName: String             = MyPostgresProfile.getClass.getName.stripSuffix("$")
    override val profileIsObject: Boolean        = true
  }
}
