# Plan 003: Propagate git preview-fetch failures instead of returning empty

> **Executor instructions**: Follow this plan step by step. Run every verification
> command and confirm the expected result before moving to the next step. If anything in
> the "STOP conditions" section occurs, stop and report — do not improvise. When done,
> update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**:
> `git diff --stat 10e852b..HEAD -- app/cli/GitCLI.scala app/service/artifact/ModuleCatalogService.scala app/service/artifact/ExamLoadService.scala app/service/artifact/ExamListService.scala app/service/artifact/ModulePreview.scala`
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

`GitCLI.getAllModulesFromPreview()` updates the local preview branch (`git fetch` +
`switch` + `reset --hard`) and then parses every module file. When the git update fails
(non-zero exit — network blip, auth failure, corrupt local checkout), it returns
`(Vector.empty, Vector.empty)` with the comment `// proper error handling`. Callers cannot
distinguish "the preview branch legitimately has no matching modules" from "git failed".
The result: on a transient git failure, exam lists, exam-load CSVs, and module-catalog PDFs
are generated **as if the modules didn't exist** — silently producing incomplete official
documents instead of surfacing an error.

After this plan, a failed preview update raises a descriptive error that propagates to the
request as a 5xx (with the controller's existing temp-file cleanup still running), instead
of a silently-empty artifact.

## Current state

Files and roles:

- `app/cli/GitCLI.scala` — `getAllModulesFromPreview()` returns empty on git failure.
- `app/service/artifact/ModulePreview.scala` — thin wrapper filtering preview modules by PO;
  calls `gitCli.getAllModulesFromPreview()`.
- `app/service/artifact/ModuleCatalogService.scala` — computes preview modules
  **synchronously** at the top of `generateCatalog`, before its `Future` comprehension.
- `app/service/artifact/ExamLoadService.scala` — computes preview modules **synchronously**
  in `createLatestExamLoad`, before its `Future` comprehension.
- `app/service/artifact/ExamListService.scala` — computes preview modules **inside** a
  `flatMap` (already in a `Future` context, so a thrown error already becomes a failed
  `Future` there — no change needed).

The silent-failure branch (`app/cli/GitCLI.scala:36-55`):

```36:55:app/cli/GitCLI.scala
  def getAllModulesFromPreview(): (Vector[ParsingError], Vector[(ModuleProtocol, LocalDate)]) = {
    val exitCode = updatePreviewBranch()

    if exitCode == 0 then {
      gitFolder
        .getFilesOfDirectory(_.getFileName.toString.endsWith(".md")) { f =>
          val lastModified = Files
            .getLastModifiedTime(f)
            .toInstant
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate
          val content = Files.readString(f)
          RawModuleParser.parser.parse(content)._1.map(_ -> lastModified)
        }
        .partitionMap(identity)
    } else {
      // proper error handling
      (Vector.empty, Vector.empty)
    }
  }
```

The two synchronous call sites that must become failed `Future`s:

`app/service/artifact/ModuleCatalogService.scala:82-101`:

```82:101:app/service/artifact/ModuleCatalogService.scala
    val modulePreview = new ModulePreview(gitCLI)
    val modules       = modulePreview
      .getAllFromPreviewByPOWithLastModified(po)
      .filterNot((m, _) => bannedGenericModules.contains(m.id.get))
    val lang                     = Lang(Locale.GERMANY)
    val moduleDiffs: ModuleDiffs = List.empty // TODO: reimplement
    val studyPrograms            = studyProgramViewRepo.notExpired().map { all =>
      val poOnly = all.filter(_.po.id == po)
      assume(poOnly.nonEmpty, s"expected study programs for po $po")
      (all, poOnly)
    }

    for {
      (all, poOnly) <- studyPrograms
      latexSnippets = getLatexSnippets(latexFile.getParent, po, moduleDiffs, isPreview)
      _             = copyAssets(latexFile.getParent)
      content <- print(poOnly, modules, all, lang, moduleDiffs, latexSnippets, semester)
      path = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    } yield pdf
```

`app/service/artifact/ExamLoadService.scala:68-75`:

```68:75:app/service/artifact/ExamLoadService.scala
  def createLatestExamLoad(po: String): Future[String] = {
    val assessmentMethods               = assessmentMethodRepo.all()
    val (parsedModules, parsedChildren) = getModulesFromPreview(po)
    val modules                         = prepareModules(parsedModules, po)
    val children                        = prepareChildren(parsedChildren, modules)
    for assessmentMethods <- assessmentMethods
    yield new ExamLoadCSVPrinter(modules, children, po, assessmentMethods).print()
  }
```

Both controllers that call these (`ModuleCatalogController.generate`,
`ExamLoadController.generateExamLoad`) wrap the service `Future` with
`.recoverWith { case NonFatal(e) => ...cleanup... }`. That cleanup only runs if the failure
is a **failed `Future`**, not a synchronous throw — hence steps 2 and 3 below.

Repo conventions: expression-oriented Scala; braces around `def`; see `AGENTS.md`.

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
- `app/cli/GitCLI.scala`
- `app/service/artifact/ModuleCatalogService.scala`
- `app/service/artifact/ExamLoadService.scala`

**Out of scope** (do NOT touch):
- `app/service/artifact/ExamListService.scala` — already invokes the preview retrieval
  inside a `Future` (`getByPo(...).flatMap { ... }`), so the new exception already becomes
  a failed `Future` there.
- `app/service/artifact/ModulePreview.scala` — leave its logic; it simply forwards the
  (now-throwing) `getAllModulesFromPreview()` result.
- `updatePreviewBranch()` in `GitCLI` — its `Int` return and process logic are unchanged.

## Git workflow

- Branch: `advisor/003-propagate-git-preview-failures`
- Commit message style: conventional commits, e.g.
  `fix: surface git preview update failures instead of empty results`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Fail loudly on a non-zero git exit

In `app/cli/GitCLI.scala`, replace the `else { (Vector.empty, Vector.empty) }` branch with a
thrown exception carrying the exit code and branch:

```scala
    } else {
      throw new IllegalStateException(
        s"failed to update preview branch '${draftBranch.value}' (git exit code $exitCode)"
      )
    }
```

Update the method's scaladoc to state that it throws when the preview branch cannot be
updated (remove the now-false "empty vector" description of the failure case).

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`. Then
`grep -n "Vector.empty, Vector.empty" app/cli/GitCLI.scala` → no matches.

### Step 2: Make `ModuleCatalogService.generateCatalog` produce a failed Future

Move the synchronous preview retrieval into the `for`-comprehension so a thrown error
becomes a failed `Future` (and the controller's `recoverWith` temp-dir cleanup runs). Wrap
it in `Future(...)`:

```scala
    val modulePreview            = new ModulePreview(gitCLI)
    val lang                     = Lang(Locale.GERMANY)
    val moduleDiffs: ModuleDiffs = List.empty // TODO: reimplement
    val studyPrograms            = studyProgramViewRepo.notExpired().map { all =>
      val poOnly = all.filter(_.po.id == po)
      assume(poOnly.nonEmpty, s"expected study programs for po $po")
      (all, poOnly)
    }

    for {
      modules <- Future(
        modulePreview
          .getAllFromPreviewByPOWithLastModified(po)
          .filterNot((m, _) => bannedGenericModules.contains(m.id.get))
      )
      (all, poOnly) <- studyPrograms
      latexSnippets = getLatexSnippets(latexFile.getParent, po, moduleDiffs, isPreview)
      _             = copyAssets(latexFile.getParent)
      content <- print(poOnly, modules, all, lang, moduleDiffs, latexSnippets, semester)
      path = Files.writeString(latexFile, content.toString)
      pdf <- compile(path).flatMap(_ => getPdf(path)).toFuture
    } yield pdf
```

`Future` and an `ExecutionContext` are already in scope in this service (it builds `Future`s
throughout). `Future(expr)` requires the implicit `ExecutionContext` that is already
present.

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`.

### Step 3: Make `ExamLoadService.createLatestExamLoad` produce a failed Future

Wrap the synchronous preview retrieval in `Future(...)` and pull it into the comprehension:

```scala
  def createLatestExamLoad(po: String): Future[String] = {
    val assessmentMethods = assessmentMethodRepo.all()
    for {
      assessmentMethods              <- assessmentMethods
      (parsedModules, parsedChildren) <- Future(getModulesFromPreview(po))
      modules  = prepareModules(parsedModules, po)
      children = prepareChildren(parsedChildren, modules)
    } yield new ExamLoadCSVPrinter(modules, children, po, assessmentMethods).print()
  }
```

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`.

### Step 4: Format and run the full suite

`sbt -Dsbt.log.noformat=true scalafmtAll` then `sbt -Dsbt.log.noformat=true test`.

**Verify**: `sbt -Dsbt.log.noformat=true test` → `All tests passed.`, exit 0.

## Test plan

- No new unit test: `getAllModulesFromPreview` shells out to real `git` and the local
  checkout; there is no seam in this project to inject a fake git process, and adding one is
  a larger refactor out of scope here. The change is verified by compilation, the existing
  suite staying green (success path unchanged), and code review confirming the failure path
  now throws rather than returning empties.
- If, while doing this, you find it trivial to extract `updatePreviewBranch`'s exit code
  into an injectable seam (it is not, given the current design), do NOT — that is a separate
  refactor. Report it as a follow-up instead.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `sbt -Dsbt.log.noformat=true compile` exits 0
- [ ] `sbt -Dsbt.log.noformat=true test` exits 0 (no regressions)
- [ ] `grep -n "Vector.empty, Vector.empty" app/cli/GitCLI.scala` → no matches
- [ ] `grep -n "Future(" app/service/artifact/ExamLoadService.scala` → at least one match
      (preview retrieval now wrapped)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 003 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The "Current state" excerpts don't match the live code (drift).
- `sbt compile` fails to resolve `nebulak` (missing `GITHUB_TOKEN`) — environment issue.
- Wrapping the preview retrieval in `Future(...)` causes a type-inference error in the
  `for`-comprehension you cannot resolve in one attempt — report the compiler output; the
  intended change is purely "run the same synchronous code inside a `Future`".
- You discover a *fourth* caller of `getAllModulesFromPreview` / `ModulePreview` beyond the
  three services named here (grep `getAllModulesFromPreview` and `new ModulePreview`) — it
  may also need the failed-Future treatment; report it.

## Maintenance notes

- The exception is intentionally coarse (any non-zero git exit → failure). If callers ever
  need to distinguish "branch missing" from "network error", introduce a typed error at the
  `updatePreviewBranch` boundary then.
- A cleaner long-term design injects the git-process runner so this path becomes unit
  testable; deferred (would touch `GitCLI` construction in `settings/GuiceInstanceProviders`).
- Reviewer should confirm the two wrapped call sites still pass the same `modules` value
  into the printers, and that temp-dir cleanup in the controllers now runs on git failure.
