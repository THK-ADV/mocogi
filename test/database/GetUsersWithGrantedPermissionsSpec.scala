package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.ModuleUpdatePermissionRepository PersonsWithGrantedUpdatePermission]]. */
final class GetUsersWithGrantedPermissionsSpec extends DatabaseSnapshotSuite {

  test("pp: one granted") {
    assertSnapshot("database/expected/get_users_granted/pp.txt") {
      sql"""SELECT jsonb_pretty(modules.get_users_with_granted_permissions_from_module('e37c5af9-6076-4f15-8c8b-d206b7091bc0'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("fsios: none granted") {
    assertSnapshot("database/expected/get_users_granted/fsios.txt") {
      sql"""SELECT jsonb_pretty(modules.get_users_with_granted_permissions_from_module('6d7e31f7-0b9e-4162-be4e-89a977c0a9ed'::uuid)::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("missing module => null") {
    assertSnapshot("database/expected/get_users_granted/unknown_module.txt") {
      sql"""SELECT modules.get_users_with_granted_permissions_from_module(${fakeUUID}::uuid)"""
        .as[String]
        .head
    }
  }
}
