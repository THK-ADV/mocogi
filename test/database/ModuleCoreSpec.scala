package database

import database.MyPostgresProfile.api.*

/** Snapshot for [[database.repo.JSONRepository.allModuleCore]] */
final class ModuleCoreSpec extends DatabaseSnapshotSuite {

  test("module ids are unique") {
    val duplicates = TestDb.runSync(
      sql"""
          SELECT
            elem->>'id' AS module_id,
            count(*)::int AS occurrences
          FROM modules.module_core,
               jsonb_array_elements(modules) AS elem
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

  test("full module_core json (published and unpublished)") {
    assertSnapshot("database/expected/module_core/all.txt") {
      sql"""SELECT jsonb_pretty(row_to_json(m)::jsonb)::text FROM modules.module_core m"""
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
