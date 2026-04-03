package database

import java.nio.file.Files
import java.nio.file.Path

import scala.io.Source

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import database.MyPostgresProfile.api.*

/**
 * Snapshot tests: compare SQL output to files under `test/resources/database/expected/` (local only, gitignored).
 *
 * Use the same Slick/slick-pg patterns as app code ([[database.repo.JSONRepository]], etc.):
 * bound `sql` parameters e.g. `${id.toString}::uuid`, and `MyAPI.setUUIDArray` when passing `uuid[]`.
 *
 * Workflow:
 *   1. `./scripts/sync-test-db-from-prod.sh`
 *   2. `sbt it:test` — or one suite from the shell: `sbt "it:testOnly database.YourSpec"`
 *   3. Refresh goldens: `UPDATE_SNAPSHOTS=1 sbt it:test`
 */
abstract class DatabaseSnapshotSuite extends AnyFunSuite, Matchers, BeforeAndAfterAll {

  override def beforeAll(): Unit =
    TestDb.start()

  protected def norm(s: String): String =
    s.trim.replace("\r\n", "\n")

  /** @param rel path under `test/resources`, e.g. `database/expected/get_module_details/generic.txt` */
  protected def assertSnapshot(rel: String)(io: => DBIO[String]): Unit = {
    val actual = norm(TestDb.runSync(io))
    val path   = Path.of(System.getProperty("user.dir"), "test/resources", rel)
    if (updateSnapshotsEnabled) {
      Files.createDirectories(path.getParent)
      Files.writeString(path, actual + "\n")
    } else {
      val expected = readExpectedText(rel, path)
      withClue(s"snapshot [$rel]\n")(actual shouldEqual expected)
    }
  }

  private def readExpectedText(rel: String, fsPath: Path): String =
    if Files.isRegularFile(fsPath) then norm(Files.readString(fsPath))
    else
      Option(getClass.getClassLoader.getResourceAsStream(rel)) match
        case Some(s) =>
          try norm(Source.fromInputStream(s, "UTF-8").mkString)
          finally s.close()
        case None =>
          throw new IllegalStateException(
            s"missing expected golden $fsPath — run UPDATE_SNAPSHOTS=1 sbt it:test after ./scripts/sync-test-db-from-prod.sh"
          )

  private def updateSnapshotsEnabled: Boolean =
    sys.env.get("UPDATE_SNAPSHOTS").exists(v => v == "1" || v.equalsIgnoreCase("true"))
      || sys.props.get("updateSnapshots").contains("1")
}
