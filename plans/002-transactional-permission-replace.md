# Plan 002: Make `ModuleUpdatePermissionService.replace` transactional

> **Executor instructions**: Follow this plan step by step. Run every verification
> command and confirm the expected result before moving to the next step. If anything in
> the "STOP conditions" section occurs, stop and report — do not improvise. When done,
> update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**:
> `git diff --stat 10e852b..HEAD -- app/service/ModuleUpdatePermissionService.scala app/database/repo/ModuleUpdatePermissionRepository.scala`
> If any in-scope file changed since this plan was written, compare the "Current state"
> excerpts against the live code before proceeding; on a mismatch, treat it as a STOP
> condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: correctness
- **Planned at**: commit `10e852b`, 2026-07-02

## Why this matters

`ModuleUpdatePermissionService.replace` rebuilds a module's edit permissions by running
**three separate database calls** — delete-by-campus-id, delete-by-kind, then insert — each
as its own `db.run`, with no transaction spanning them. If the process crashes or a query
fails after the deletes but before the insert, the module is left with **no edit
permissions at all** until the next successful sync. This path runs on every main-branch
module sync (via `ModuleDatabaseActor` → `overrideInherited`), on module creation (via
`ModuleCreationService`), and on the user-facing "grant permissions" endpoint
(`ModuleUpdatePermissionController`). A dropped permission set silently locks authors and
reviewers out of a module.

After this plan the delete+delete+insert runs as a single Slick `.transactionally` action:
it either fully succeeds or leaves the previous permissions untouched.

## Current state

Files and roles:

- `app/service/ModuleUpdatePermissionService.scala` — `replace(module, campusIds, kind)`
  orchestrates the three repo calls in a `for`-comprehension over `Future`s (non-atomic).
- `app/database/repo/ModuleUpdatePermissionRepository.scala` — the repository; already has
  `delete`, `deleteInherited`, `deleteGranted`, and inherits `createMany` from
  `database.repo.Repository`. Table PK is composite `(module, campusId)`.

The non-atomic service method (`app/service/ModuleUpdatePermissionService.scala:36-47`):

```36:47:app/service/ModuleUpdatePermissionService.scala
  def replace(
      module: UUID,
      campusIds: Seq[CampusId],
      kind: ModuleUpdatePermissionType
  ) =
    for {
      _ <- repo.delete(module, campusIds)
      _ <- kind match
        case ModuleUpdatePermissionType.Inherited => repo.deleteInherited(module)
        case ModuleUpdatePermissionType.Granted   => repo.deleteGranted(module)
      _ <- repo.createMany(campusIds.distinct.map(c => (module, c, kind)))
    } yield ()
```

The three repo pieces it composes (`app/database/repo/ModuleUpdatePermissionRepository.scala:58-72`):

```58:72:app/database/repo/ModuleUpdatePermissionRepository.scala
  def delete(
      module: UUID,
      campusIds: Seq[CampusId]
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(a => a.module === module && a.campusId.inSet(campusIds))
        .delete
    )

  def deleteInherited(module: UUID) =
    db.run(tableQuery.filter(a => a.module === module && a.isInherited).delete)

  def deleteGranted(module: UUID) =
    db.run(tableQuery.filter(a => a.module === module && a.isGranted).delete)
```

The repository's row type is `(UUID, CampusId, ModuleUpdatePermissionType)`. It already has
`import profile.api.*`, `import database.table.campusIdColumnType`,
`import database.table.moduleUpdatePermissionTypeColumnType`, and `import models.*` (which
brings in `ModuleUpdatePermissionType`), plus `import auth.CampusId`.

**Exemplar** — the repo pattern for a transactional delete-then-insert already exists in
this codebase; copy its shape (`app/database/repo/schedule/ModuleTeachingUnitRepository.scala:45-52`):

```45:52:app/database/repo/schedule/ModuleTeachingUnitRepository.scala
      _ <- db.run(
        DBIO
          .seq(
            TableQuery[ModuleTeachingUnitTable].filter(_.module.inSet(modules.map(_._1))).delete,
            TableQuery[ModuleTeachingUnitTable].insertAll(entries),
          )
          .transactionally
      )
```

Repo conventions to match: expression-oriented Scala; braces around `def`; `match { ... }`;
see `AGENTS.md`. This repo already prefers direct Slick `DBIO` actions.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `sbt -Dsbt.log.noformat=true compile` | `[success]`, exit 0 |
| Unit tests | `sbt -Dsbt.log.noformat=true test` | `All tests passed.`, exit 0 |
| Format | `sbt -Dsbt.log.noformat=true scalafmtAll` | exit 0 |

> Building requires `GITHUB_TOKEN` (read:packages) for the private `nebulak` dependency.
> A resolution failure on `de.th-koeln.inf.adv:nebulak` is the missing-token problem, not a
> code error — STOP and report (see plan 007).

## Scope

**In scope** (the only files you should modify):
- `app/database/repo/ModuleUpdatePermissionRepository.scala`
- `app/service/ModuleUpdatePermissionService.scala`

**Out of scope** (do NOT touch, even though they look related):
- `app/git/subscriber/ModuleDatabaseActor.scala`, `app/service/ModuleCreationService.scala`,
  `app/controllers/ModuleUpdatePermissionController.scala` — callers of `replace`; their
  signatures and behavior stay identical (the method keeps the same shape).
- The existing `delete` / `deleteInherited` / `deleteGranted` methods — leave them; they
  are used elsewhere. You are ADDING a transactional path, not removing the old methods.
- The cross-module atomicity of `overrideInherited` (it calls `replace` per module in a
  `Future.sequence`) — out of scope; per-module atomicity is the goal here.

## Git workflow

- Branch: `advisor/002-transactional-permission-replace`
- Commit message style: conventional commits, e.g.
  `fix: make module update permission replace transactional`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add a transactional `replace` to the repository

In `app/database/repo/ModuleUpdatePermissionRepository.scala`, add a new method that
composes the three operations into one `.transactionally` action. Preserve the exact
semantics of the current service method: delete rows matching the incoming campus ids
(any kind), delete all rows of `kind` for the module, then insert the new `(module,
campusId, kind)` rows for the distinct campus ids.

```scala
  def replace(
      module: UUID,
      campusIds: Seq[CampusId],
      kind: ModuleUpdatePermissionType
  ): Future[Unit] = {
    val entries = campusIds.distinct.map(c => (module, c, kind))
    val deleteByCampusId =
      tableQuery.filter(a => a.module === module && a.campusId.inSet(campusIds)).delete
    val deleteByKind = kind match {
      case ModuleUpdatePermissionType.Inherited =>
        tableQuery.filter(a => a.module === module && a.isInherited).delete
      case ModuleUpdatePermissionType.Granted =>
        tableQuery.filter(a => a.module === module && a.isGranted).delete
    }
    db.run(
      DBIO.seq(deleteByCampusId, deleteByKind, tableQuery ++= entries).transactionally
    ).map(_ => ())
  }
```

Notes:
- `tableQuery ++= entries` is Slick's batch insert (returns `DBIO[Option[Int]]`); it is the
  transactional equivalent of the base `createMany`. `DBIO.seq(...)` discards intermediate
  results and yields `DBIO[Unit]`, so the trailing `.map(_ => ())` keeps the `Future[Unit]`
  return type. (If `DBIO.seq` already yields `Unit` and the compiler rejects the `.map`,
  drop the `.map(_ => ())`.)
- Do not remove or rename the existing `delete`/`deleteInherited`/`deleteGranted` methods.

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`, exit 0.

### Step 2: Delegate the service method to the repository

In `app/service/ModuleUpdatePermissionService.scala`, replace the body of `replace` so it
calls the new repository method instead of orchestrating three futures:

```scala
  def replace(
      module: UUID,
      campusIds: Seq[CampusId],
      kind: ModuleUpdatePermissionType
  ): Future[Unit] =
    repo.replace(module, campusIds, kind)
```

Leave `overrideInherited`, `hasPermissionFor`, and every other method unchanged. If the
`ModuleUpdatePermissionType` import becomes unused after this change, the compiler's
`-Wunused:imports` (enabled in `build.sbt`) will flag it — remove only a genuinely unused
import if the build warns. (It is still used in the method signature here, so it should
remain.)

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`, exit 0.

### Step 3: Format and run the full suite

`sbt -Dsbt.log.noformat=true scalafmtAll` then `sbt -Dsbt.log.noformat=true test`.

**Verify**: `sbt -Dsbt.log.noformat=true test` → `All tests passed.`, exit 0. No pre-existing
tests should break (the observable behavior of `replace` is unchanged on the success path).

## Test plan

- No new unit test is added here: the change is a persistence-atomicity guarantee, and this
  repository talks to real PostgreSQL (slick-pg types; there is no in-memory DB harness in
  this project — the DB suite under `test/database` runs via `sbt it:test` against a
  restored Postgres, per `README.md`).
- Regression safety comes from the existing `sbt test` suite continuing to pass (success-path
  behavior is identical) plus code review of the transactional composition.
- **Optional deeper verification** (only if a Postgres test DB is available, see
  `scripts/sync-test-db-from-prod.sh` and `test/database/TestDb.scala`): add a
  `ModuleUpdatePermissionRepositorySpec` under `test/database/` that seeds a module with
  granted permissions, calls `replace`, and asserts the final row set — following the
  structure of an existing spec such as `test/database/GetUsersWithGrantedPermissionsSpec.scala`.
  Do NOT attempt this if no test DB is configured; report it as deferred instead.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `sbt -Dsbt.log.noformat=true compile` exits 0
- [ ] `sbt -Dsbt.log.noformat=true test` exits 0 (no regressions)
- [ ] `grep -n "transactionally" app/database/repo/ModuleUpdatePermissionRepository.scala`
      returns a match
- [ ] `app/service/ModuleUpdatePermissionService.scala` `replace` body is a single call to
      `repo.replace(...)` (no `for`-comprehension over three repo calls)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 002 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The "Current state" excerpts don't match the live code (drift since this plan was written).
- `sbt compile` fails to resolve `nebulak` (missing `GITHUB_TOKEN`) — environment issue.
- `tableQuery ++= entries` does not type-check in this Slick/slick-pg version — report the
  compiler error; the fallback is `DBIO.sequence(entries.map(e => tableQuery += e))` inside
  the same `.transactionally` block, but confirm before switching.
- Any verification fails twice after a reasonable fix attempt.

## Maintenance notes

- If `overrideInherited`'s per-module loop ever needs to be atomic *across* modules (all or
  nothing for a whole sync batch), that is a larger change: it would need a single
  transaction spanning all modules, which conflicts with the current per-module `replace`
  boundary. Revisit then.
- Reviewer should scrutinize that the delete/insert ordering inside the transaction matches
  the old sequence (delete-by-campus, delete-by-kind, insert) and that no rows the old code
  preserved are now being deleted.
- Related deferred finding (see `plans/README.md`): several other sync pipelines
  (`CoreDataPublisher`, `ModuleDatabaseActor`, `ModuleReviewService`, `MergeEventHandler`)
  have the same non-transactional multi-write shape.
