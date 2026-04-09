package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.ModuleUpdatePermissionRepository.isModulePartOfPO]]. */
final class ModuleOfPoSpec extends DatabaseSnapshotSuite {

  test("NULL args => false") {
    TestDb.runSync(sql"""SELECT modules.module_of_po(NULL::uuid, ARRAY['x']::text[])""".as[Boolean].head) shouldBe false
    TestDb.runSync(sql"SELECT modules.module_of_po(${fakeUUID}::uuid, NULL::text[])".as[Boolean].head) shouldBe false
  }

  test("published module belongs to one of given POs") {
    TestDb.runSync(
      sql"SELECT modules.module_of_po('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid, ARRAY['inf_inf2']::text[])::text"
        .as[Boolean]
        .head
    ) shouldBe true
  }

  test("published module does not belong to given POs") {
    TestDb.runSync(
      sql"SELECT modules.module_of_po('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid, ARRAY['ing_een5']::text[])::text"
        .as[Boolean]
        .head
    ) shouldBe false
  }

  test("published module belongs to some of given POs") {
    TestDb.runSync(
      sql"SELECT modules.module_of_po('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid, ARRAY['inf_inf2', 'inf_mi5']::text[])::text"
        .as[Boolean]
        .head
    ) shouldBe true
  }

  test("unpublished module belongs to one PO") {
    TestDb.runSync(
      sql"SELECT modules.module_of_po('3f28e778-e437-4672-a907-be679b44c9bb'::uuid, ARRAY['ing_een5']::text[])::text"
        .as[Boolean]
        .head
    ) shouldBe true
  }

  test("unpublished module does not belong to given POs") {
    TestDb.runSync(
      sql"SELECT modules.module_of_po('e32db28f-ef69-4919-aa1b-eaabcde500ee'::uuid, ARRAY['ing_een5']::text[])::text"
        .as[Boolean]
        .head
    ) shouldBe false
  }
}
