package database

import database.MyPostgresProfile.api.*

/** Snapshot for [[database.repo.JSONRepository.allModuleCore]] */
final class ModuleCoreSpec extends DatabaseSnapshotSuite {

  test("module ids are unique (published and unpublished)") {
    val duplicates = TestDb.runSync(
      sql"""
          SELECT
            elem->>'id' AS module_id,
            count(*)::int AS occurrences
          FROM jsonb_array_elements(modules.module_core(true)) AS elem
          GROUP BY elem->>'id'
          HAVING count(*) > 1
          ORDER BY module_id
        """.as[(String, Int)]
    )

    withClue(
      s"Found duplicate module ids: ${duplicates.map { case (id, count) => s"$id($count)" }.mkString(", ")}"
    ) {
      duplicates shouldBe empty
    }
  }

  test("module ids are unique (published only)") {
    val duplicates = TestDb.runSync(
      sql"""
          SELECT
            elem->>'id' AS module_id,
            count(*)::int AS occurrences
          FROM jsonb_array_elements(modules.module_core(false)) AS elem
          GROUP BY elem->>'id'
          HAVING count(*) > 1
          ORDER BY module_id
        """.as[(String, Int)]
    )

    withClue(
      s"Found duplicate module ids: ${duplicates.map { case (id, count) => s"$id($count)" }.mkString(", ")}"
    ) {
      duplicates shouldBe empty
    }
  }

  test("published only result contains no draft modules") {
    val draftCount = TestDb.runSync(
      sql"""
          SELECT count(*)::int
          FROM jsonb_array_elements(modules.module_core(false)) AS elem
          WHERE (elem->>'isLive')::boolean IS NOT TRUE
        """.as[Int].head
    )

    withClue(s"Found $draftCount non-live modules in published-only result") {
      draftCount shouldBe 0
    }
  }

  test("full module_core json (published and unpublished)") {
    assertSnapshot("database/expected/module_core/all.txt") {
      sql"""SELECT jsonb_pretty(
              jsonb_build_object('modules', modules.module_core(true))
            )::text"""
        .as[String]
        .head
    }
  }

  test("module_core json (published only)") {
    assertSnapshot("database/expected/module_core/live.txt") {
      sql"""SELECT jsonb_pretty(
              jsonb_build_object('modules', modules.module_core(false))
            )::text"""
        .as[String]
        .head
    }
  }

  test("module_core raw json (published only)") {
    assertSnapshot("database/expected/module_core/raw.txt") {
      sql"""SELECT jsonb_pretty(
            COALESCE(
              jsonb_agg(to_jsonb(m) ORDER BY m.id),
              '[]'::jsonb
            )
          )::text
          FROM modules.module_core_raw m""".as[String].head
    }
  }
}
