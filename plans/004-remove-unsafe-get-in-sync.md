# Plan 004: Replace unsafe `.get` in module publish / teaching-unit sync

> **Executor instructions**: Follow this plan step by step. Run every verification
> command and confirm the expected result before moving to the next step. If anything in
> the "STOP conditions" section occurs, stop and report — do not improvise. When done,
> update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**:
> `git diff --stat 10e852b..HEAD -- app/git/publisher/ModulePublisher.scala app/database/repo/schedule/ModuleTeachingUnitRepository.scala`
> If any in-scope file changed since this plan was written, compare the "Current state"
> excerpts against the live code before proceeding; on a mismatch, treat it as a STOP
> condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: correctness
- **Planned at**: commit `10e852b`, 2026-07-02

## Why this matters

Two hot sync paths call `.get` on an `Option` that can be empty, turning a recoverable
condition into a batch-aborting `NoSuchElementException`:

1. **`ModulePublisher`** (runs on every push that changes modules): after parsing/validating
   a batch, it pairs each parsed module back to its git file with
   `changes.find(_._1.id == m.metadata.id).get._1`. If normalization ever changes a module's
   id so it no longer matches an incoming change, the `.get` throws inside the actor's
   success callback and **the entire batch of modules is dropped** — none reach the database
   subscribers, and only a generic failure is logged.
2. **`ModuleTeachingUnitRepository.update`** (runs on every main-branch module sync): it looks
   up the INF and ING teaching-unit seed rows with `.find(_.isINF).get.id` /
   `.find(_.isING).get.id`. On a fresh or partially-migrated environment where those seed
   rows are absent, the whole sync step fails with an opaque `NoSuchElementException` (after
   earlier sync steps may have already committed).

After this plan, a module that can't be matched is logged and skipped (the rest of the batch
proceeds), and a missing teaching-unit seed produces a clear, actionable error message.

## Current state

The `ModulePublisher` pairing (`app/git/publisher/ModulePublisher.scala:41-64`):

```41:64:app/git/publisher/ModulePublisher.scala
      pipeline.parseValidateMany(prints).onComplete {
        case Success(validates) =>
          val modules = validates.map(_.map {
            case (_, module) =>
              val m = module.normalized()
              val f = changes.find(_._1.id == m.metadata.id).get._1
              (m, f)
          })
          modules match {
            case Right(modules) =>
              subscribers.handle(modules, correlationId)
              infoEvent(
                event = event,
                result = LogResult.Succeeded,
                correlationId = correlationId,
                details = Map("moduleCount" -> modules.size.toString)
              )
            case Left(errs) =>
              logPipelineErrors(event, correlationId, errs)
          }
        case Failure(t) =>
          logFutureFailure(event, correlationId, t)
      }
```

Here `changes: List[(GitFile.ModuleFile, GitFileContent)]` (from `NotifySubscribers`);
`_._1` is the `GitFile.ModuleFile`. The outer `validates.map(_.map { ... })` maps over the
`Either`'s `Right` (a `Seq`); the inner `.map` transforms each element. `ModulePublisher`
extends `play.api.Logging`, so `logger` is available.

The teaching-unit lookup (`app/database/repo/schedule/ModuleTeachingUnitRepository.scala:30-53`):

```30:53:app/database/repo/schedule/ModuleTeachingUnitRepository.scala
  def update(modules: Seq[(UUID, List[String])]): Future[Unit] =
    for {
      teachingUnits <- db.run(TableQuery[TeachingUnitTable].result)
      entries = {
        val inf = teachingUnits.find(_.isINF).get.id
        val ing = teachingUnits.find(_.isING).get.id
        modules.map { (module, pos) =>
          val tus = mutable.Set.empty[UUID]
          for (po <- pos) {
            if po.startsWith("inf") then tus.add(inf)
            if po.startsWith("ing") then tus.add(ing)
          }
          ModuleTeachingUnit(module, tus.toList)
        }
      }
      _ <- db.run(
        DBIO
          .seq(
            TableQuery[ModuleTeachingUnitTable].filter(_.module.inSet(modules.map(_._1))).delete,
            TableQuery[ModuleTeachingUnitTable].insertAll(entries),
          )
          .transactionally
      )
    } yield ()
```

A throw inside the `entries = { ... }` binding of this `for`-comprehension propagates as a
**failed `Future`** (the binding runs inside the desugared `.map`), so throwing a clear
exception is a valid, contained fix here.

Repo conventions: expression-oriented Scala; braces around `def`; `match { ... }`; see
`AGENTS.md`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `sbt -Dsbt.log.noformat=true compile` | `[success]`, exit 0 |
| Unit tests | `sbt -Dsbt.log.noformat=true test` | `All tests passed.`, exit 0 |
| Format | `sbt -Dsbt.log.noformat=true scalafmtAll` | exit 0 |

> Building requires `GITHUB_TOKEN` (read:packages). A `nebulak` resolution failure is the
> missing-token problem, not a code error — STOP and report (see plan 007).

## Scope

**In scope** (the only files you should modify):
- `app/git/publisher/ModulePublisher.scala`
- `app/database/repo/schedule/ModuleTeachingUnitRepository.scala`

**Out of scope** (do NOT touch, even though they contain similar `.get` patterns):
- `app/webhook/MergeEventHandler.scala:393` (`diff.newPath.moduleId(gitConfig).get`) — a
  different `.get` site with different semantics; noted as a follow-up, not part of this plan.
- The `subscribers.handle` contract and `ModuleSubscribers` — unchanged; you only change how
  the `Seq` passed to it is built.

## Git workflow

- Branch: `advisor/004-remove-unsafe-get-in-sync`
- Commit message style: conventional commits, e.g.
  `fix: skip unmatched modules and report missing teaching-unit seeds`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Skip (don't crash on) unmatched modules in `ModulePublisher`

Change the inner `.map` to `.flatMap` returning an `Option`, logging and dropping any module
whose id doesn't match an incoming change instead of throwing:

```scala
          val modules = validates.map(_.flatMap {
            case (_, module) =>
              val m = module.normalized()
              changes.find(_._1.id == m.metadata.id) match {
                case Some(change) => Some((m, change._1))
                case None         =>
                  logger.error(
                    s"skipping module ${m.metadata.id}: no matching git change after normalization"
                  )
                  None
              }
          })
```

`Seq#flatMap` with an `Option` result keeps the `Some` elements and drops the `None`s, so
the surrounding `modules match { case Right(modules) => ... }` continues to work unchanged
(it still receives a `Seq` of successfully-paired modules).

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`. Then
`grep -n ".get._1" app/git/publisher/ModulePublisher.scala` → no matches.

### Step 2: Report missing teaching-unit seeds with a clear message

Replace the two `.get` lookups with `getOrElse`-throw carrying an actionable message.
Behavior for a correctly-seeded database is identical; only the error case changes from an
opaque `NoSuchElementException` to a descriptive one:

```scala
      entries = {
        val inf = teachingUnits
          .find(_.isINF)
          .map(_.id)
          .getOrElse(throw new IllegalStateException(
            "teaching unit 'INF' seed row not found in teaching_unit table"
          ))
        val ing = teachingUnits
          .find(_.isING)
          .map(_.id)
          .getOrElse(throw new IllegalStateException(
            "teaching unit 'ING' seed row not found in teaching_unit table"
          ))
        modules.map { (module, pos) =>
          val tus = mutable.Set.empty[UUID]
          for (po <- pos) {
            if po.startsWith("inf") then tus.add(inf)
            if po.startsWith("ing") then tus.add(ing)
          }
          ModuleTeachingUnit(module, tus.toList)
        }
      }
```

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`. Then
`grep -n ".get.id" app/database/repo/schedule/ModuleTeachingUnitRepository.scala` → no
matches.

### Step 3: Format and run the full suite

`sbt -Dsbt.log.noformat=true scalafmtAll` then `sbt -Dsbt.log.noformat=true test`.

**Verify**: `sbt -Dsbt.log.noformat=true test` → `All tests passed.`, exit 0.

## Test plan

- No new unit test: both sites live inside an actor callback / DB-backed repository method
  with no injection seam, and the change is defensive (skip/clear-error) rather than a new
  behavior with an observable pure output. Verification is compilation, the existing suite
  staying green, and the greps in Done criteria confirming the `.get` calls are gone.
- If a Postgres test DB is available, an optional `ModuleTeachingUnitRepositorySpec` under
  `test/database/` could assert that `update` fails with the new message when the
  `teaching_unit` table is empty — but only attempt this if a test DB is configured (see
  `test/database/TestDb.scala`); otherwise report as deferred.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `sbt -Dsbt.log.noformat=true compile` exits 0
- [ ] `sbt -Dsbt.log.noformat=true test` exits 0 (no regressions)
- [ ] `grep -n ".get._1" app/git/publisher/ModulePublisher.scala` → no matches
- [ ] `grep -n ".get.id" app/database/repo/schedule/ModuleTeachingUnitRepository.scala` → no matches
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 004 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The "Current state" excerpts don't match the live code (drift).
- `sbt compile` fails to resolve `nebulak` (missing `GITHUB_TOKEN`) — environment issue.
- Changing `.map` to `.flatMap` in `ModulePublisher` produces a type error you can't resolve
  in one attempt (the `Right` branch must still be a `Seq[(ModuleProtocol, GitFile.ModuleFile)]`)
  — report the compiler output.
- Any verification fails twice after a reasonable fix attempt.

## Maintenance notes

- Related deferred `.get` site: `app/webhook/MergeEventHandler.scala:393`
  (`diff.newPath.moduleId(gitConfig).get`) throws if a changed file path can't be parsed to a
  module id, aborting a bulk merge. Same class of bug; fix in a follow-up if it recurs.
- Reviewer should confirm the `ModulePublisher` change still passes a non-empty `Seq` to
  `subscribers.handle` in the normal case and that dropped modules are visible in logs.
- If teaching-unit seeds become configurable rather than fixed INF/ING rows, revisit Step 2.
