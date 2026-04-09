package database

import database.MyPostgresProfile.api.*

final class ViewSpec extends DatabaseSnapshotSuite {

  test("full module view as json") {
    assertSnapshot("database/expected/view/module_view.txt") {
      sql"""SELECT jsonb_pretty(coalesce(jsonb_agg(to_jsonb(m) ORDER BY m.id), '[]'::jsonb))::text FROM modules.module_view m"""
        .as[String]
        .head
    }
  }

  test("full study program view as json") {
    assertSnapshot("database/expected/view/study_program_view.txt") {
      sql"""SELECT jsonb_pretty(coalesce(jsonb_agg(to_jsonb(m) ORDER BY m.po_id), '[]'::jsonb))::text FROM core.study_program_view m"""
        .as[String]
        .head
    }
  }

  test("full study program view currently active as json") {
    assertSnapshot("database/expected/view/study_program_view_currently_active.txt") {
      sql"""SELECT jsonb_pretty(coalesce(jsonb_agg(to_jsonb(m) ORDER BY m.po_id), '[]'::jsonb))::text FROM core.study_program_view_currently_active m"""
        .as[String]
        .head
    }
  }

  test("full study program view not expired as json") {
    assertSnapshot("database/expected/view/study_program_view_not_expired.txt") {
      sql"""SELECT jsonb_pretty(coalesce(jsonb_agg(to_jsonb(m) ORDER BY m.po_id), '[]'::jsonb))::text FROM core.study_program_view_not_expired m"""
        .as[String]
        .head
    }
  }
}
