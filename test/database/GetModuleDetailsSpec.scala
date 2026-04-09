package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.JSONRepository.getModuleDetails]]. */
final class GetModuleDetailsSpec extends DatabaseSnapshotSuite {

  test("module with mandatory and optional POs") {
    assertSnapshot("database/expected/get_module_details/pp.txt") {
      sql"""SELECT jsonb_pretty(modules.get_module_details('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("generic module") {
    assertSnapshot("database/expected/get_module_details/generic.txt") {
      sql"""SELECT jsonb_pretty(modules.get_module_details('8305a1c4-806b-47b9-a99f-e8cebea5211f'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("instance of generic module with only optional POs") {
    assertSnapshot("database/expected/get_module_details/elective_module.txt") {
      sql"""SELECT jsonb_pretty(modules.get_module_details('696858c3-ce09-4dd7-8449-09bcd8a860a2'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("inactive module") {
    assertSnapshot("database/expected/get_module_details/inactive.txt") {
      sql"""SELECT jsonb_pretty(modules.get_module_details('6d7e31f7-0b9e-4162-be4e-89a977c0a9ed'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("missing module => null") {
    TestDb.runSync(
      sql"""SELECT modules.get_module_details(${fakeUUID}::uuid)"""
        .as[Option[String]]
        .head
    ) shouldBe None
  }
}
