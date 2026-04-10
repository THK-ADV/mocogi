package database

import java.util.UUID

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.JSONRepository.genericModuleOptions]]. */
final class GetGenericModuleOptionsSpec extends DatabaseSnapshotSuite {

  test("get all modules which are an instance of wasp1") {
    assertSnapshot("database/expected/get_generic_module_options/wasp1.txt") {
      sql"""SELECT jsonb_pretty(modules.get_generic_module_options('8305a1c4-806b-47b9-a99f-e8cebea5211f'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("get all modules which are an instance of wasp2") {
    assertSnapshot("database/expected/get_generic_module_options/wasp2.txt") {
      sql"""SELECT jsonb_pretty(modules.get_generic_module_options('ed95a06c-c83a-4e5a-92bd-3c6fa960bdcd'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("fails for non-generic module") {
    val nonGenericId = UUID.fromString(
      TestDb.runSync(
        sql"""SELECT id::text FROM modules.module WHERE module_type = 'module' LIMIT 1""".as[String].head
      )
    )
    assertSnapshot("database/expected/get_generic_module_options/non_generic.txt") {
      sql"""SELECT jsonb_pretty(modules.get_generic_module_options(${nonGenericId.toString}::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }
}
