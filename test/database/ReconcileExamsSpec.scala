package database

import java.util.UUID

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import database.repo.ModuleRepository
import database.table.ModuleTable
import database.MyPostgresProfile.api.*
import database.MyPostgresProfile.MyAPI.setUUIDArray
import play.api.db.slick.DatabaseConfigProvider
import slick.basic.BasicProfile
import slick.basic.DatabaseConfig

/** Integration tests of [[ModuleRepository.reconcilingExams]] and [[ModuleTable.hasOwnExams]]. */
final class ReconcileExamsSpec extends ExamFixtures {

  protected def idPrefix = 3

  private lazy val repository = ModuleRepository(databaseConfigProvider, summon)

  private val parent        = id(1)
  private val becameChild   = id(2) // had own exams, now a child of parent
  private val generic       = id(3)
  private val gainedOwn     = id(4) // was a child of parent without exams, relation removed
  private val named         = id(5)
  private val untouchedKid  = id(6) // stale exams of a child, not passed to reconcilingExams
  private val untouchedGen  = id(7) // stale exams of a generic module, not passed to reconcilingExams
  private val lostAll       = id(8) // has own exams, but no assignment
  private val all           = List(parent, becameChild, generic, gainedOwn, named, untouchedKid, untouchedGen, lostAll)
  private val reconciled    = List(parent, becameChild, generic, gainedOwn, named, lostAll)
  private val unreconciled  = Set(untouchedKid, untouchedGen)
  private val namedExams    = List(named -> "Klausur", named -> "Projekt")
  private val staleExams    = List(untouchedKid -> "Stale Child", untouchedGen -> "Stale")
  private val reconciledAll = List(parent -> "Parent", gainedOwn -> "Gained Own") ++ namedExams

  /** Stages the modules and their exams before a sync. */
  private val fixtures = DBIO.seq(
    insertModule(parent, "Parent"),
    insertModule(becameChild, "Became Child"),
    insertModule(generic, "Generic", generic = true),
    insertModule(gainedOwn, "Gained Own"),
    insertModule(named, "Named"),
    insertModule(untouchedKid, "Untouched Child"),
    insertModule(untouchedGen, "Untouched Generic", generic = true),
    insertModule(lostAll, "Lost All"),
    relate(parent, gainedOwn),
    exam(id(101), "Parent", parent),
    exam(id(102), "Became Child", becameChild),
    exam(id(103), "Klausur", becameChild, generic, named),
    exam(id(104), "Projekt", named),
    exam(id(105), "Generic", generic),
    relate(parent, untouchedKid),
    exam(id(106), "Stale Child", untouchedKid),
    exam(id(107), "Stale", untouchedGen)
  )

  /** The sync that changes types and relations. */
  private val sync = DBIO.seq(
    relate(parent, becameChild),
    sqlu"DELETE FROM modules.module_relation WHERE child = ${gainedOwn.toString}::uuid"
  )

  private val exams: DBIO[List[(UUID, String)]] =
    sql"""SELECT assignment.module::text, exam.name
          FROM exams.exam_module assignment JOIN exams.exam exam ON exam.id = assignment.exam
          WHERE assignment.module = ANY ($all::uuid[])
          ORDER BY 1, 2""".as[(String, String)].map(_.map((m, n) => (UUID.fromString(m), n)).toList)

  test("hasOwnExams is false for child and generic modules only") {
    val result = rollback(
      fixtures >> sync >> TableQuery[ModuleTable].filter(_.id.inSet(all)).map(m => (m.id, m.hasOwnExams)).result
    )

    result.toMap shouldBe Map(
      parent       -> true,
      becameChild  -> false,
      generic      -> false,
      gainedOwn    -> true,
      named        -> true,
      untouchedKid -> false,
      untouchedGen -> false,
      lostAll      -> true
    )
  }

  test("removes exams of child and generic modules, adds one named after a module that gained own exams") {
    val result = rollback(fixtures >> repository.reconcilingExams(reconciled)(sync) >> exams)

    result shouldBe (reconciledAll ++ staleExams).sortBy(identity)
  }

  test("is idempotent and leaves modules that are not given untouched") {
    val (first, second) = rollback(for {
      _      <- fixtures
      _      <- repository.reconcilingExams(reconciled)(sync)
      first  <- exams
      _      <- repository.reconcilingExams(reconciled)(DBIO.unit)
      second <- exams
    } yield (first, second))

    second shouldBe first
    first.filter(e => unreconciled(e._1)) shouldBe staleExams
  }

  test("an empty argument changes nothing") {
    val (before, after) = rollback(for {
      _      <- fixtures
      before <- exams
      _      <- repository.reconcilingExams(Nil)(DBIO.unit)
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
