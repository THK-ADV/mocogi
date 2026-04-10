package database

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Using

import com.zaxxer.hikari.HikariDataSource
import database.MyPostgresProfile.api.*
import slick.util.AsyncExecutor

/**
 * JDBC pool for DB snapshot tests.
 *
 * Data is not loaded from the repo: run `./scripts/sync-test-db-from-prod.sh` (or your own restore)
 * so `TEST_JDBC_URL` points at a DB that is a copy of production schema + data.
 *
 * Env: `TEST_JDBC_URL` (default `jdbc:postgresql://localhost:5432/mocogi_test`), `TEST_DB_USER`,
 * `TEST_DB_PASSWORD`, `TEST_DB_POOL`.
 */
object TestDb {

  val profile: MyPostgresProfile.type = MyPostgresProfile

  private val jdbcUrl = sys.env.getOrElse("TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/mocogi_test")
  private val dbUser  = sys.env.getOrElse("TEST_DB_USER", "postgres")
  private val dbPass  = sys.env.getOrElse("TEST_DB_PASSWORD", "")
  private val maxPool = sys.env.getOrElse("TEST_DB_POOL", "4").toInt

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  private val startLock = new Object
  private var started   = false

  lazy val dataSource: HikariDataSource = {
    val ds = new HikariDataSource()
    ds.setJdbcUrl(jdbcUrl)
    ds.setUsername(dbUser)
    ds.setPassword(dbPass)
    ds.setMaximumPoolSize(maxPool)
    ds
  }

  lazy val db: Database = {
    start()
    profile.backend.Database.forDataSource(
      dataSource,
      Some(maxPool),
      AsyncExecutor.default(),
      keepAliveConnection = false
    )
  }

  /** Verifies connectivity once per JVM. */
  def start(): Unit = startLock.synchronized {
    if (!started) {
      Using.resource(dataSource.getConnection)(c =>
        Using.resource(c.createStatement())(_.executeQuery("SELECT 1").next())
      )
      started = true
    }
  }

  def run[R](action: DBIO[R]): Future[R] =
    db.run(action)

  def runSync[R](action: DBIO[R]): R = {
    import scala.concurrent.duration.*
    import scala.concurrent.Await
    Await.result(db.run(action), 120.seconds)
  }
}
