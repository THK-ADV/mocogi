package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.PermissionRepository]] → `modules.get_user_info`. */
final class GetUserInfoSpec extends DatabaseSnapshotSuite {

  test("pav") {
    assertSnapshot("database/expected/get_user_info/pav.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.get_user_info('dga', 'dgaida2') t"""
        .as[String]
        .head
    }
  }

  test("sgl") {
    assertSnapshot("database/expected/get_user_info/sgl.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.get_user_info('rba', 'rbartnik') t"""
        .as[String]
        .head
    }
  }

  test("mv") {
    assertSnapshot("database/expected/get_user_info/mv.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.get_user_info('cko', 'ckohls') t"""
        .as[String]
        .head
    }
  }

  test("rejected review") {
    assertSnapshot("database/expected/get_user_info/rejected_review.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.get_user_info('lkoe', 'lkoehler') t"""
        .as[String]
        .head
    }
  }

  test("pending review") {
    assertSnapshot("database/expected/get_user_info/pending_review.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.get_user_info('nmo', 'nmuell22') t"""
        .as[String]
        .head
    }
  }

  test("unknown user => empty row shape still valid json") {
    assertSnapshot("database/expected/get_user_info/unknown_user.txt") {
      sql"""SELECT jsonb_pretty(to_jsonb(t.*))::text FROM modules.get_user_info('__no_user__', '__no_user__') t"""
        .as[String]
        .head
    }
  }
}
