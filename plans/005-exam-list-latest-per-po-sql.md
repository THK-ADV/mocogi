# Plan 005: Compute exam-list "latest per PO" in SQL

> **Executor instructions**: Follow this plan step by step. Run every verification
> command and confirm the expected result before moving to the next step. If anything in
> the "STOP conditions" section occurs, stop and report — do not improvise. When done,
> update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**:
> `git diff --stat 10e852b..HEAD -- app/database/repo/ExamListRepository.scala app/models/Semester.scala`
> If any in-scope file changed since this plan was written, compare the "Current state"
> excerpts against the live code before proceeding; on a mismatch, treat it as a STOP
> condition.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: MED
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `10e852b`, 2026-07-02

## Why this matters

`ExamListRepository.eachLatest()` (backing `GET /examLists`) joins the **entire**
`exam_list` table to the study-program view, loads every historical row into the JVM, then
groups by PO and picks the latest semester per PO in Scala. The code's own comment flags it:
"this implementation is very inefficient, because the filtering happens in scala." Cost
grows with every stored semester. This plan pushes the "latest row per PO" selection into
PostgreSQL (`DISTINCT ON`), so the query returns at most one row per PO regardless of
history depth.

**The subtlety that makes this MED-risk, not trivial:** semesters are stored as strings like
`wise_2025` / `sose_2025`, and their chronological order is **not** lexicographic. The domain
ordering (`models/Semester.scala`) is: by year ascending, and for the same year Winter
(`wise`) is later than Summer (`sose`). A naive `ORDER BY semester DESC` would be wrong
(`sose_2025` would sort before `wise_2024` alphabetically, but `sose_2025` is actually the
later semester). The SQL below reproduces the domain ordering exactly.

## Current state

`app/database/repo/ExamListRepository.scala:30-52`:

```30:52:app/database/repo/ExamListRepository.scala
  /**
   * TODO: this implementation is very inefficient, because the filtering happens in scala.
   * TODO: A native psql function should be better
   */
  def eachLatest(): Future[Seq[ExamList]] = {
    val studyProgramView = studyProgramViewRepository.tableQuery.filter(_.specializationId.isEmpty)
    val now              = LocalDate.now
    val current          = Semester.of(now).id
    val query            = tableQuery
      .join(studyProgramView)
      .on(_.po === _.poId)
      .result
      .map(
        _.groupBy(_._1.po)
          .map {
            case (po, xs) =>
              val (examList, studyProgram) = xs.maxBy(e => Semester(e._1.semester))
              ExamList(studyProgram, Semester(examList.semester), examList.date, examList.url)
          }
          .toSeq
      )
    db.run(query)
  }
```

(Note: the local `val current` is computed but never used — it can be dropped.)

Domain ordering to reproduce (`app/models/Semester.scala:24-34`):

```24:34:app/models/Semester.scala
  given Ordering[Semester] = (lhs, rhs) => {
    val yearRes = lhs.year.compareTo(rhs.year)
    if yearRes == 0 then {
      // wise is always the latest semester if the year is the same
      (lhs.abbrev, rhs.abbrev) match {
        case ("wise", "sose") => 1
        case ("sose", "wise") => -1
        case _                => 0
      }
    } else yearRes
  }
```

Relevant types and facts:

- `ExamListDbEntry(po: String, semester: String, date: LocalDate, url: String)` is defined in
  `app/database/table/ExamListTable.scala` as `private[database]` — accessible from
  `database.repo` (same top-level `database` package). The current repo already references it.
- The `exam_list` table lives in schema `modules` (`database.Schema.Modules.name == "modules"`);
  the table name is `exam_list`.
- `StudyProgramView` (`app/models/StudyProgramView.scala`) has `.po: POCore` (with `.id`) and
  `.specialization: Option[IDLabel]`.
- `StudyProgramViewRepository` (`app/database/view/StudyProgramViewRepository.scala`) exposes
  `notExpired(): Future[Seq[StudyProgramView]]` — this returns exactly the set the current
  code joins against (`tableQuery` in that repo IS `notExpiredTableQuery`), so
  `notExpired().filter(_.specialization.isEmpty)` equals the current join's right side.
- The repository already has `import profile.api.*` and an implicit `ExecutionContext`.

Repo conventions: this repo already uses raw `sql"..."` in sibling repos (e.g.
`ModuleUpdatePermissionRepository.allForUser`). Expression-oriented Scala; see `AGENTS.md`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `sbt -Dsbt.log.noformat=true compile` | `[success]`, exit 0 |
| Unit tests | `sbt -Dsbt.log.noformat=true test` | `All tests passed.`, exit 0 |
| Format | `sbt -Dsbt.log.noformat=true scalafmtAll` | exit 0 |
| DB snapshot suite (optional, needs Postgres) | `sbt -Dsbt.log.noformat=true it:test` | all pass — only if a test DB is restored |

> Building requires `GITHUB_TOKEN` (read:packages). A `nebulak` resolution failure is the
> missing-token problem — STOP and report (see plan 007).
> `it:test` requires a restored Postgres (`scripts/sync-test-db-from-prod.sh`); do NOT run it
> if no DB is configured.

## Scope

**In scope** (the only files you should modify):
- `app/database/repo/ExamListRepository.scala`

**Out of scope** (do NOT touch):
- `app/models/Semester.scala` — read-only reference for the ordering.
- `app/database/view/StudyProgramViewRepository.scala` — reuse `notExpired()` as-is.
- `createOrUpdate` in `ExamListRepository` — unchanged.
- The `ExamList` model and the `GET /examLists` response shape — must stay identical.

## Git workflow

- Branch: `advisor/005-exam-list-latest-per-po-sql`
- Commit message style: conventional commits, e.g.
  `perf: compute latest exam list per po in sql`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add a `DISTINCT ON` query for the latest exam-list row per PO

In `app/database/repo/ExamListRepository.scala`, add the imports and a plain-SQL query. Read
`date` via `nextDate().toLocalDate` (plain JDBC — avoids depending on java.time `GetResult`
implicits):

```scala
import slick.jdbc.GetResult
import database.Schema
```

```scala
  private given GetResult[ExamListDbEntry] =
    GetResult(r => ExamListDbEntry(r.nextString(), r.nextString(), r.nextDate().toLocalDate, r.nextString()))

  // Latest exam-list row per PO. Semester ids ("wise_2025"/"sose_2025") are NOT ordered
  // lexicographically: order by year, then Winter (wise) after Summer (sose) within a year —
  // matching Ordering[Semester] in models/Semester.scala.
  private def latestPerPoQuery =
    sql"""
      SELECT DISTINCT ON (po) po, semester, date, url
      FROM #${Schema.Modules.name}.exam_list
      ORDER BY po,
               split_part(semester, '_', 2)::int DESC,
               (CASE WHEN split_part(semester, '_', 1) = 'wise' THEN 1 ELSE 0 END) DESC
    """.as[ExamListDbEntry]
```

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`.

### Step 2: Rewrite `eachLatest()` to use the query + reuse `notExpired()`

Replace the body of `eachLatest()` with a two-query assembly (latest exam-list rows +
non-specialization study programs), joined in memory by PO id:

```scala
  def eachLatest(): Future[Seq[ExamList]] =
    for {
      latest        <- db.run(latestPerPoQuery)
      studyPrograms <- studyProgramViewRepository.notExpired()
    } yield {
      val byPo = studyPrograms.filter(_.specialization.isEmpty).map(sp => sp.po.id -> sp).toMap
      latest.flatMap(e => byPo.get(e.po).map(sp => ExamList(sp, Semester(e.semester), e.date, e.url)))
    }
```

Remove the now-unused `now`/`current` locals and the old `tableQuery.join(...)` block. Leave
`createOrUpdate` and the class header untouched. If `LocalDate` is no longer referenced,
`-Wunused:imports` will flag it — remove the import only if the build warns.

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`. Then
`grep -n "groupBy" app/database/repo/ExamListRepository.scala` → no matches (the in-memory
grouping is gone).

### Step 3: Format and run the unit suite

`sbt -Dsbt.log.noformat=true scalafmtAll` then `sbt -Dsbt.log.noformat=true test`.

**Verify**: `sbt -Dsbt.log.noformat=true test` → `All tests passed.`, exit 0.

### Step 4 (only if a Postgres test DB is available): validate the SQL ordering

The unit suite does not exercise real SQL. If — and only if — a restored test DB is present
(`scripts/sync-test-db-from-prod.sh` was run), sanity-check the `DISTINCT ON` result against
the old logic before trusting it. Run this read-only query in `psql` and confirm it returns
exactly one row per `po`, each carrying that PO's chronologically latest semester (year, then
wise-after-sose):

```sql
SELECT DISTINCT ON (po) po, semester
FROM modules.exam_list
ORDER BY po,
         split_part(semester, '_', 2)::int DESC,
         (CASE WHEN split_part(semester, '_', 1) = 'wise' THEN 1 ELSE 0 END) DESC;
```

Spot-check any PO that has both a `sose_YYYY` and a `wise_YYYY` row for the same year: the
result must show `wise_YYYY`. If it doesn't, the ordering is wrong — STOP.

**Verify**: one row per PO; latest semester per the domain ordering.

## Test plan

- No pure unit test is added: the value of this change is the SQL, which requires a live
  PostgreSQL to exercise (this repo has no in-memory DB harness; the DB suite under
  `test/database` runs via `sbt it:test` against restored Postgres — see `README.md`).
- Regression safety: the existing `sbt test` suite must stay green, and the response shape of
  `GET /examLists` is unchanged (`Seq[ExamList]`).
- Optional: if a test DB exists, add a `ExamListRepositorySpec` under `test/database/`
  seeding one PO with `sose_2024`, `wise_2024`, `sose_2025` and asserting `eachLatest`
  returns the `sose_2025` row (latest by year) — model it after an existing DB spec such as
  `test/database/GetModulesForPoSpec.scala`. Only attempt with a configured test DB.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `sbt -Dsbt.log.noformat=true compile` exits 0
- [ ] `sbt -Dsbt.log.noformat=true test` exits 0 (no regressions)
- [ ] `grep -n "DISTINCT ON" app/database/repo/ExamListRepository.scala` returns a match
- [ ] `grep -n "groupBy" app/database/repo/ExamListRepository.scala` → no matches
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 005 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The "Current state" excerpts don't match the live code (drift).
- `sbt compile` fails to resolve `nebulak` (missing `GITHUB_TOKEN`) — environment issue.
- The `GetResult` / `sql"..."` interpolation does not compile in this Slick version after one
  fix attempt — report the compiler error. (Do not silently fall back to the old in-memory
  approach; report so the maintainer can decide.)
- A test DB is available and Step 4 shows the `DISTINCT ON` picking the wrong semester for any
  PO — the ordering expression is wrong; STOP rather than shipping incorrect exam lists.
- You find that a single PO maps to more than one non-specialization study program in
  `study_program_view` (which would make the `byPo` map lossy) — report it; the current code
  has the same ambiguity but confirm before assuming 1:1.

## Maintenance notes

- The ordering expression is duplicated knowledge with `Ordering[Semester]`. If the semester
  id format or ordering rule ever changes (e.g. trimester), both must change together — leave
  the explanatory comment in the SQL so the next maintainer sees the coupling.
- If exam lists ever need to be returned for specialization study programs too, the
  `notExpired().filter(_.specialization.isEmpty)` filter and the `byPo` keying must be
  revisited.
- Reviewer should scrutinize the semester ordering SQL against `models/Semester.scala` and
  confirm the response shape and PII exposure of `GET /examLists` are unchanged.
