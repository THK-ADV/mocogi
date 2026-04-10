package database

import database.MyPostgresProfile.api.*

final class ViewSpec extends DatabaseSnapshotSuite {

  test("full module view as json") {
    assertSnapshot("database/expected/view/module_view.txt") {
      sql"""
        SELECT jsonb_pretty(
          coalesce(
            jsonb_agg(
              jsonb_build_object(
                'id', m.id,
                'title', m.title,
                'abbrev', m.abbrev,
                'ects', m.ects,
                'status', m.status,
                'moduleManagement', coalesce((
                  SELECT
                    jsonb_agg(entry ORDER BY entry ->> 'id')
                  FROM
                    jsonb_array_elements(m."moduleManagement") AS module_management(entry)
                ), '[]'::jsonb),
                'studyProgram', coalesce((
                  SELECT
                    jsonb_agg(
                      entry
                      ORDER BY
                        entry -> 'studyProgram' ->> 'id',
                        entry -> 'studyProgram' -> 'po' ->> 'id',
                        (entry -> 'studyProgram' -> 'po' ->> 'version')::int,
                        coalesce(entry -> 'studyProgram' -> 'specialization' ->> 'id', ''),
                        (entry ->> 'mandatory')::boolean DESC,
                        (entry -> 'recommendedSemester')::text
                    )
                  FROM
                    jsonb_array_elements(m."studyProgram") AS study_program(entry)
                ), '[]'::jsonb)
              )
              ORDER BY m.id
            ),
            '[]'::jsonb
          )
        )::text
        FROM modules.module_view m
      """
        .as[String]
        .head
    }
  }

  test("module view study-program associations stay deduplicated") {
    val duplicatedModules = TestDb.runSync(
      sql"""
        SELECT
          count(*)
        FROM
          modules.module_view m
        WHERE
          jsonb_array_length(m."studyProgram") <> (
            SELECT
              count(*)
            FROM (
              SELECT DISTINCT
                entry
              FROM
                jsonb_array_elements(m."studyProgram") AS study_program(entry)
            ) deduped
          )
      """.as[Int].head
    )

    duplicatedModules shouldEqual 0
  }

  test("module view keeps recommended semester order from source tables") {
    val mismatches = TestDb.runSync(
      sql"""
        WITH actual AS (
          SELECT
            m.id AS module_id,
            entry -> 'studyProgram' ->> 'id' AS sp_id,
            entry -> 'studyProgram' -> 'po' ->> 'id' AS po_id,
            (entry -> 'studyProgram' -> 'po' ->> 'version')::int AS po_version,
            entry -> 'studyProgram' -> 'specialization' ->> 'id' AS spec_id,
            (entry ->> 'mandatory')::boolean AS mandatory,
            entry -> 'recommendedSemester' AS recommended_semester
          FROM
            modules.module_view m
            CROSS JOIN LATERAL jsonb_array_elements(m."studyProgram") AS study_program(entry)
        ),
        expected AS (
          SELECT
            module_po_mandatory.module AS module_id,
            study_program_view_not_expired.sp_id,
            study_program_view_not_expired.po_id,
            study_program_view_not_expired.po_version,
            study_program_view_not_expired.spec_id,
            TRUE AS mandatory,
            to_jsonb(module_po_mandatory.recommended_semester) AS recommended_semester
          FROM
            modules.module_po_mandatory
            JOIN core.study_program_view_not_expired ON study_program_view_not_expired.po_id = module_po_mandatory.po
              AND CASE WHEN module_po_mandatory.specialization IS NOT NULL THEN
                study_program_view_not_expired.spec_id = module_po_mandatory.specialization
              ELSE
                TRUE
              END
          UNION
          SELECT
            module_po_optional.module AS module_id,
            study_program_view_not_expired.sp_id,
            study_program_view_not_expired.po_id,
            study_program_view_not_expired.po_version,
            study_program_view_not_expired.spec_id,
            FALSE AS mandatory,
            to_jsonb(module_po_optional.recommended_semester) AS recommended_semester
          FROM
            modules.module_po_optional
            JOIN core.study_program_view_not_expired ON study_program_view_not_expired.po_id = module_po_optional.po
              AND CASE WHEN module_po_optional.specialization IS NOT NULL THEN
                study_program_view_not_expired.spec_id = module_po_optional.specialization
              ELSE
                TRUE
              END
        ),
        diff AS (
          SELECT
            *
          FROM
            actual
          EXCEPT
          SELECT
            *
          FROM
            expected
          UNION
          SELECT
            *
          FROM
            expected
          EXCEPT
          SELECT
            *
          FROM
            actual
        )
        SELECT
          count(*)
        FROM
          diff
      """.as[Int].head
    )

    mismatches shouldEqual 0
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
