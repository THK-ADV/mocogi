package database

import database.MyPostgresProfile.api.*

/** Snapshots for [[database.repo.JSONRepository.semesterPlan]]. */
final class SemesterPlanByNowSpec extends DatabaseSnapshotSuite {

  test("March 2026") {
    assertSnapshot("database/expected/semester_plan_by_now/march_2026.txt") {
      sql"""SELECT jsonb_pretty(schedule.semester_plan_by_now(3, 2026)::jsonb)::text""".as[String].head
    }
  }

  test("October 2026") {
    assertSnapshot("database/expected/semester_plan_by_now/october_2026.txt") {
      sql"""SELECT jsonb_pretty(schedule.semester_plan_by_now(10, 2026)::jsonb)::text""".as[String].head
    }
  }
}
