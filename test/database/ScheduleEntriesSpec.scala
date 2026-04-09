package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.schedule.ScheduleEntryRepository]] SQL functions. */
final class ScheduleEntriesSpec extends DatabaseSnapshotSuite {

  test("by ids") {
    assertSnapshot("database/expected/schedule_entries/by_ids.txt") {
      sql"""SELECT jsonb_pretty(schedule.get_schedule_entries(ARRAY['db711fad-39b5-4b72-8d4d-8d0c992f8e3f'::uuid,'20a2523d-f8c0-4893-b5f8-a72c603e9c0e'::uuid,'6ff26c44-41d9-49b7-bd68-7187b516e369'::uuid,'29e5d08a-0f29-49d9-8607-9edaf14346de'::uuid,'fd491392-bcd4-48cd-a2cd-7b4e5fb6e64c'::uuid])::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("from 6. to 11. Apr. 2026") {
    assertSnapshot("database/expected/schedule_entries/block-week-sose-26.txt") {
      sql"""SELECT jsonb_pretty(schedule.get_schedule_entries(
            TIMESTAMP '2026-04-06 00:00:00',
            TIMESTAMP '2026-04-11 00:00:00'
          )::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("from 13. to 18. Apr. 2026") {
    assertSnapshot("database/expected/schedule_entries/exam-week-sose-26.txt") {
      sql"""SELECT jsonb_pretty(schedule.get_schedule_entries(
            TIMESTAMP '2026-04-13 00:00:00',
            TIMESTAMP '2026-04-18 00:00:00'
          )::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("from 20. to 25. Apr. 2026") {
    assertSnapshot("database/expected/schedule_entries/first-lecture-week-sose-26.txt") {
      sql"""SELECT jsonb_pretty(schedule.get_schedule_entries(
            TIMESTAMP '2026-04-20 00:00:00',
            TIMESTAMP '2026-04-25 00:00:00'
          )::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("from 25. to 30. May. 2026") {
    assertSnapshot("database/expected/schedule_entries/sixth-lecture-week-sose-26.txt") {
      sql"""SELECT jsonb_pretty(schedule.get_schedule_entries(
            TIMESTAMP '2026-05-25 00:00:00',
            TIMESTAMP '2026-05-30 00:00:00'
          )::jsonb)::text"""
        .as[String]
        .head
    }
  }

  test("from June 2026") {
    assertSnapshot("database/expected/schedule_entries/june-sose-26.txt") {
      sql"""SELECT jsonb_pretty(schedule.get_schedule_entries(
            TIMESTAMP '2026-06-01 00:00:00',
            TIMESTAMP '2026-06-30 00:00:00'
          )::jsonb)::text"""
        .as[String]
        .head
    }
  }
}
