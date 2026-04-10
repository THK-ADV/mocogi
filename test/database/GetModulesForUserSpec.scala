package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.ModuleUpdatePermissionRepository.modulesForUser]]. */
final class GetModulesForUserSpec extends DatabaseSnapshotSuite {

  test("adobryni") {
    assertSnapshot("database/expected/get_modules_for_user/adobryni.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_user('adobryni')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("umuesse") {
    assertSnapshot("database/expected/get_modules_for_user/umuesse.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_user('umuesse')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("ckohls") {
    assertSnapshot("database/expected/get_modules_for_user/ckohls.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_user('ckohls')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("jdiedric") {
    assertSnapshot("database/expected/get_modules_for_user/jdiedric.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_user('jdiedric')::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("unknown campus => empty modules list") {
    assertSnapshot("database/expected/get_modules_for_user/unknown_campus.txt") {
      sql"""SELECT jsonb_pretty(modules.get_modules_for_user('__no_campus__')::jsonb)::text"""
        .as[String]
        .head
    }
  }
}
