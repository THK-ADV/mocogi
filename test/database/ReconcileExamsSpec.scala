package database

import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import database.repo.ModuleRepository
import database.table.ModuleExamTable
import database.table.ModuleTable
import database.MyPostgresProfile.api.*
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.BasicProfile
import slick.basic.DatabaseConfig

/** Integration tests of [[ModuleRepository.reconcileExams]] and [[ModuleTable.hasOwnExams]]. */
final class ReconcileExamsSpec extends ExamFixtures {

  protected def idPrefix = 3

  private lazy val repository = ModuleRepository(databaseConfigProvider, summon)

  private val parent        = id(1)
  private val becameChild   = id(2) // had own exams, now a child of parent
  private val generic       = id(3)
  private val gainedOwn     = id(4) // was a child of parent without exams, relation removed
  private val named         = id(5)
  private val untouchedKid  = id(6) // stale exams of a child, not passed to reconcileExams
  private val untouchedGen  = id(7) // stale exams of a generic module, not passed to reconcileExams
  private val all           = List(parent, becameChild, generic, gainedOwn, named, untouchedKid, untouchedGen)
  private val reconciled    = List(parent, becameChild, generic, gainedOwn, named)
  private val unreconciled  = Set(untouchedKid, untouchedGen)
  private val namedExams    = List(named -> Some("Klausur"), named -> Some("Projekt"))
  private val staleExams    = List(untouchedKid -> None, untouchedGen -> Some("Stale"))
  private val reconciledAll = List(parent -> None, gainedOwn -> None) ++ namedExams

  /** Stages the modules in the state after a sync that changed their types and relations. */
  private val fixtures = DBIO.seq(
    insertModule(parent, "Parent"),
    insertModule(becameChild, "Became Child"),
    insertModule(generic, "Generic", generic = true),
    insertModule(gainedOwn, "Gained Own"),
    insertModule(named, "Named"),
    insertModule(untouchedKid, "Untouched Child"),
    insertModule(untouchedGen, "Untouched Generic", generic = true),
    relate(parent, gainedOwn),
    exam(parent, None),
    exam(becameChild, None),
    exam(becameChild, Some("Klausur")),
    exam(generic, None),
    exam(generic, Some("Klausur")),
    DBIO.seq(namedExams.map(exam(_, _))*),
    relate(parent, untouchedKid),
    DBIO.seq(staleExams.map(exam(_, _))*),
    // the sync
    relate(parent, becameChild),
    sqlu"DELETE FROM modules.module_relation WHERE child = ${gainedOwn.toString}::uuid"
  )

  private val exams: DBIO[List[(UUID, Option[String])]] =
    TableQuery[ModuleExamTable].filter(_.module.inSet(all)).sortBy(e => (e.module, e.name)).result.map(_.toList)

  test("hasOwnExams is false for child and generic modules only") {
    val result = rollback(
      fixtures >> TableQuery[ModuleTable].filter(_.id.inSet(all)).map(m => (m.id, m.hasOwnExams)).result
    )

    result.toMap shouldBe Map(
      parent       -> true,
      becameChild  -> false,
      generic      -> false,
      gainedOwn    -> true,
      named        -> true,
      untouchedKid -> false,
      untouchedGen -> false
    )
  }

  test("removes exams of child and generic modules, adds the default exam once and keeps named exams") {
    val result = rollback(fixtures >> repository.reconcileExams(reconciled) >> exams)

    result shouldBe (reconciledAll ++ staleExams).sortBy(_._1)
  }

  test("is idempotent and leaves modules that are not given untouched") {
    val (first, second) = rollback(for {
      _      <- fixtures
      _      <- repository.reconcileExams(reconciled)
      first  <- exams
      _      <- repository.reconcileExams(reconciled)
      second <- exams
    } yield (first, second))

    second shouldBe first
    first.filter(e => unreconciled(e._1)) shouldBe staleExams
  }

  test("an empty argument changes nothing") {
    val (before, after) = rollback(for {
      _      <- fixtures
      before <- exams
      _      <- repository.reconcileExams(Nil)
      after  <- exams
    } yield (before, after))

    after shouldBe before
  }

  private lazy val databaseConfigProvider = new DatabaseConfigProvider {
    override def get[P <: BasicProfile]: DatabaseConfig[P] = databaseConfig.asInstanceOf[DatabaseConfig[P]]
  }

  private lazy val databaseConfig = new DatabaseConfig[MyPostgresProfile.type] {
    override val profile: MyPostgresProfile.type = MyPostgresProfile
    override val db: profile.backend.Database    = TestDb.db
    override val config: Config                  = ConfigFactory.empty()
    override val profileName: String             = MyPostgresProfile.getClass.getName.stripSuffix("$")
    override val profileIsObject: Boolean        = true
  }
}
