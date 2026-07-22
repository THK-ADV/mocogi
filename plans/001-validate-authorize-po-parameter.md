# Plan 001: Validate and authorize the `po` path parameter for artifact endpoints

> **Executor instructions**: Follow this plan step by step. Run every verification
> command and confirm the expected result before moving to the next step. If anything in
> the "STOP conditions" section occurs, stop and report — do not improvise. When done,
> update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**:
> `git diff --stat 10e852b..HEAD -- app/permission/ArtifactCheck.scala app/controllers/ExamListsController.scala app/controllers/ModuleCatalogController.scala app/controllers/ExamLoadController.scala app/cli/LatexCompiler.scala app/printing/latex/WordLatexPrinter.scala`
> If any in-scope file changed since this plan was written, compare the "Current state"
> excerpts against the live code before proceeding; on a mismatch, treat it as a STOP
> condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `10e852b`, 2026-07-02

## Why this matters

The `:po` (Prüfungsordnung / examination-regulation id) route segment is taken from the
URL and flows, unvalidated, into three dangerous sinks on the artifact-generation
endpoints:

1. **Process arguments.** `LatexCompiler.compile` builds a command *string* that Scala's
   `Process(String)` splits on whitespace. A `po` containing spaces injects extra
   arguments into `latexmk` (which can execute arbitrary Perl via `-e`).
2. **Filesystem paths.** `WordLatexPrinter.initFolder(po)` resolves `po` into a directory
   path with no containment check and then calls `deleteDirectory()` on it — a `po`
   containing `..` can cause **recursive deletion of an arbitrary directory** the service
   user can write to.
3. **Authorization gap.** The artifact permission filters only check the `:studyProgram`
   segment; the `:po` segment is never checked against the caller's permitted POs, so a
   user authorized for one PO of a study program can generate/upload/export artifacts for
   a *different* PO of that study program.

Additionally, `ModuleCatalogController.uploadIntroFile` — a state-changing write — is
gated by `canPreviewArtifact` instead of `canCreateArtifact`, so preview-only users can
overwrite intro content.

After this plan: `po` is validated against a strict allowlist at a single choke point
(the artifact permission filters, which every affected endpoint already uses), the two
process/filesystem sinks are hardened as defense-in-depth, `po` is authorized against the
caller's permitted PO set, and the upload endpoint requires create permission.

## Current state

Files and roles:

- `app/permission/ArtifactCheck.scala` — trait with `canCreateArtifact(studyProgram)` and
  `canPreviewArtifact(studyProgram)` `ActionFilter`s, mixed into all artifact controllers.
  This is the single choke point.
- `app/controllers/ExamListsController.scala` — `getPreview`, `replace` (uses the filters).
- `app/controllers/ModuleCatalogController.scala` — `allGenericModulesForPO`, `generate`,
  `uploadIntroFile` (uses the filters; `uploadIntroFile` uses the wrong one).
- `app/controllers/ExamLoadController.scala` — `generateExamLoad` (uses the filter).
- `app/cli/LatexCompiler.scala` — `compile` builds the `latexmk` command as a string.
- `app/printing/latex/WordLatexPrinter.scala` — `initFolder(po)` resolves + deletes a dir.
- `app/permission/Permissions.scala` — `artifactsCreatePermissions: Set[String]` and
  `artifactsPreviewPermissions: Set[String]` return the caller's permitted PO ids (already
  resolved from teaching-unit contexts upstream). `isAdmin: Boolean`.

Current permission filter (`app/permission/ArtifactCheck.scala:19-68`):

```19:68:app/permission/ArtifactCheck.scala
  def canCreateArtifact(studyProgram: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        if request.permissions.isAdmin then Future.successful(None)
        else {
          studyProgramPrivilegesService
            .studyProgramIdsForPOs(request.permissions.artifactsCreatePermissions)
            .map { studyPrograms =>
              if studyPrograms.contains(studyProgram) then None
              else
                Some(
                  forbiddenForUser(
                    request,
                    request.request.token.username,
                    Some(s"to create artifacts for $studyProgram")
                  )
                )
            }
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }

  def canPreviewArtifact(studyProgram: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        if request.permissions.isAdmin then Future.successful(None)
        else {
          studyProgramPrivilegesService
            .studyProgramIdsForPOs(request.permissions.artifactsPreviewPermissions)
            .map { studyPrograms =>
              if studyPrograms.contains(studyProgram) then None
              else
                Some(
                  forbiddenForUser(
                    request,
                    request.request.token.username,
                    Some(s"to preview artifacts for $studyProgram")
                  )
                )
            }
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
```

`ArtifactCheck` extends `UsesClientErrors` (`app/controllers/UsesClientErrors.scala`),
which exposes `protected def clientErrors: ClientErrorResponse` and `forbiddenForUser(...)`.
`ClientErrorResponse.badRequest(request: RequestHeader, message: String): Result` exists
(`app/security/ClientErrorResponse.scala:39`). `UserRequest[A]` wraps the request; use
`request.request` for the underlying `RequestHeader` (as the existing `forbiddenForUser`
call does via `request.request.token.username`).

The vulnerable sinks:

```22:28:app/cli/LatexCompiler.scala
  def compile(file: Path): Either[String, String] = {
    val process = Process(
      command = s"latexmk -xelatex -halt-on-error ${file.getFileName.toString}",
      cwd = file.getParent.toAbsolutePath.toFile
    )
    exec(process)
  }
```

```33:37:app/printing/latex/WordLatexPrinter.scala
  private def initFolder(filename: String): Path = {
    val dir = Paths.get(outputFolder).resolve(filename)
    if Files.isDirectory(dir) then dir.deleteDirectory()
    Files.createDirectory(dir)
  }
```

The **exemplar** for safe path containment already exists in this repo — copy its shape
(`app/controllers/ExamListsController.scala:96-104`):

```96:104:app/controllers/ExamListsController.scala
  /** Only serves files that lie inside [[examListFolder]] after normalization (path traversal safe). */
  private def resolveExamListFile(filename: String): Option[Path] =
    val trimmed = filename.trim
    if trimmed.isEmpty || trimmed != filename || trimmed.indexOf('\u0000') >= 0 then None
    else {
      val base     = Paths.get(examListFolder).toAbsolutePath.normalize()
      val resolved = base.resolve(trimmed).normalize()
      Option.when(resolved.startsWith(base))(resolved)
    }
```

Repo conventions to match: expression-oriented Scala; braces around `object`/`class`/
`def` blocks; `match { ... }`; single-expression `def f(...) = expr`. See `AGENTS.md`.
Known PO id shapes (from `docs/README.md`): `inf_mi5`, `inf_mim5`, `inf_mi4`, `inf_dsi1` —
lowercase letters, digits, underscores.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `sbt -Dsbt.log.noformat=true compile` | `[success]`, exit 0 |
| Unit tests | `sbt -Dsbt.log.noformat=true test` | `All tests passed.`, exit 0 |
| Format | `sbt -Dsbt.log.noformat=true scalafmtAll` | exit 0 |

> Building requires the `GITHUB_TOKEN` env var (read:packages) for the private `nebulak`
> dependency. If `sbt compile` fails to resolve `de.th-koeln.inf.adv:nebulak`, that is the
> missing-token problem, not a code error — STOP and report (see plan 007).

## Scope

**In scope** (the only files you should modify):
- `app/permission/ArtifactCheck.scala`
- `app/controllers/ExamListsController.scala`
- `app/controllers/ModuleCatalogController.scala`
- `app/controllers/ExamLoadController.scala`
- `app/cli/LatexCompiler.scala`
- `app/printing/latex/WordLatexPrinter.scala`
- `test/permission/PoFormatSpec.scala` (create)

**Out of scope** (do NOT touch, even though they look related):
- The service layer (`app/service/artifact/*`) — the fix is at the filter + sink level.
- `app/controllers/ExamListsController.scala` `getFile`/`resolveExamListFile` — already
  path-traversal safe; leave as-is (use it only as the pattern to copy).
- `app/database/repo/*` — `po` reaching DB queries is already parameterized/safe.
- Any change to the response shape of successful artifact generation.

## Git workflow

- Branch: `advisor/001-validate-po-parameter`
- Commit per step; message style: conventional commits (repo uses `fix:`, `feat:`,
  `refactor:` — e.g. `fix: validate and authorize po path parameter`).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add a strict `po` format validator with a shared choke point

In `app/permission/ArtifactCheck.scala`, add a companion `object ArtifactCheck` (or a
private helper in the trait) with a validation function and regex:

```scala
object ArtifactCheck {
  // PO ids are lowercase-ish identifiers (e.g. inf_mi5, inf_mim5). Disallow anything that
  // could inject process arguments (whitespace) or traverse the filesystem ('/', '..').
  private val PoPattern = "^[A-Za-z0-9_-]{1,64}$".r

  def isValidPo(po: String): Boolean = PoPattern.matches(po)
}
```

Then change **both** `canCreateArtifact` and `canPreviewArtifact` to take an additional
`po: String` parameter and, as the *first* thing inside `filter`, reject a malformed `po`
with `400` before any other logic:

```scala
  def canCreateArtifact(studyProgram: String, po: String) =
    new ActionFilter[UserRequest] {
      protected override def filter[A](request: UserRequest[A]): Future[Option[Result]] = {
        if !ArtifactCheck.isValidPo(po) then
          Future.successful(Some(clientErrors.badRequest(request.request, s"invalid po id: $po")))
        else if request.permissions.isAdmin then Future.successful(None)
        else {
          studyProgramPrivilegesService
            .studyProgramIdsForPOs(request.permissions.artifactsCreatePermissions)
            .map { studyPrograms =>
              // Bind BOTH segments: the study program must be permitted AND the requested
              // po must be one the caller actually holds an artifacts-create permission for.
              if studyPrograms.contains(studyProgram)
                && request.permissions.artifactsCreatePermissions.contains(po)
              then None
              else
                Some(
                  forbiddenForUser(
                    request,
                    request.request.token.username,
                    Some(s"to create artifacts for $studyProgram / $po")
                  )
                )
            }
        }
      }

      protected override def executionContext: ExecutionContext = ctx
    }
```

Apply the mirror change to `canPreviewArtifact`, using
`request.permissions.artifactsPreviewPermissions` for BOTH the `studyProgramIdsForPOs`
call and the new `.contains(po)` check (keep the existing wording "to preview artifacts").

Add the needed import if not present: the trait already extends `UsesClientErrors`, so
`clientErrors` and `forbiddenForUser` are in scope; no new import required for those.

**Verify**: `sbt -Dsbt.log.noformat=true compile` → fails with "not enough arguments" /
"missing argument" errors at the call sites in the three controllers. That is expected —
you fix them in Step 2. (This confirms you found all call sites.)

### Step 2: Update all six call sites to pass `po`

Pass the route `po` to the filter at each call site. The controllers already have `po` in
scope as a method parameter.

- `app/controllers/ExamListsController.scala:60` — `.andThen(canPreviewArtifact(studyProgram, po))`
- `app/controllers/ExamListsController.scala:116` — `.andThen(canCreateArtifact(studyProgram, po))`
- `app/controllers/ModuleCatalogController.scala:68` — `.andThen(canPreviewArtifact(studyProgram, po))`
- `app/controllers/ModuleCatalogController.scala:82` — `.andThen(canPreviewArtifact(studyProgram, po))`
- `app/controllers/ExamLoadController.scala:45` — `.andThen(canPreviewArtifact(studyProgram, po))`

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`, exit 0.

### Step 3: Fix the upload endpoint to require create permission (SECURITY-06)

In `app/controllers/ModuleCatalogController.scala`, `uploadIntroFile` (currently line 160)
is a state-changing write but uses `canPreviewArtifact`. Change it to:

```scala
      .andThen(canCreateArtifact(studyProgram, po))
```

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`. Then
`grep -n "canPreviewArtifact(studyProgram, po)" app/controllers/ModuleCatalogController.scala`
→ returns the two read endpoints only (lines for `allGenericModulesForPO` and `generate`),
not `uploadIntroFile`.

### Step 4: Harden `LatexCompiler.compile` to not tokenize (defense-in-depth)

In `app/cli/LatexCompiler.scala`, replace the string-command `Process(...)` with the
sequence form so arguments are never re-split on whitespace:

```scala
  def compile(file: Path): Either[String, String] = {
    val process = Process(
      command = Seq("latexmk", "-xelatex", "-halt-on-error", file.getFileName.toString),
      cwd = file.getParent.toAbsolutePath.toFile
    )
    exec(process)
  }
```

`scala.sys.process.Process` has an overload taking `Seq[String]` + `cwd: File`; the
existing `import scala.sys.process.*` / `import scala.sys.process.Process` already covers
it. `exec(process: ProcessBuilder)` is unchanged.

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`.

### Step 5: Add path containment to `WordLatexPrinter.initFolder` (defense-in-depth)

In `app/printing/latex/WordLatexPrinter.scala`, make `initFolder` reject any `filename`
that escapes `outputFolder`, mirroring `resolveExamListFile`. The method returns `Path`;
throw a clear exception on violation (the caller `toLatex` already wraps everything in
`Try`, so a thrown exception becomes a `Failure` and is surfaced as a 400 by the
controller's `case Failure(e) => clientErrors.badRequest(...)`).

```scala
  private def initFolder(filename: String): Path = {
    val base     = Paths.get(outputFolder).toAbsolutePath.normalize()
    val resolved = base.resolve(filename).normalize()
    if !resolved.startsWith(base) then
      throw new IllegalArgumentException(s"invalid intro path for: $filename")
    if Files.isDirectory(resolved) then resolved.deleteDirectory()
    Files.createDirectory(resolved)
  }
```

Note: with Step 1's validation, a malformed `po` is already rejected at the filter before
reaching here; this step is the second layer in case `WordLatexPrinter` is ever called
from a path that does not go through `ArtifactCheck`.

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`.

### Step 6: Add a unit test for the `po` validator

Create `test/permission/PoFormatSpec.scala`. Model it structurally after an existing
plain ScalaTest spec such as `test/controllers/ModuleRelationFormatSpec.scala` (read it
first for the exact base class / style the repo uses — typically
`org.scalatest.wordspec.AnyWordSpec with org.scalatest.matchers.should.Matchers`).

Cover:
- accepts real PO ids: `inf_mi5`, `inf_mim5`, `inf_mi4`, `inf_dsi1`
- rejects an id containing a space (argument-injection vector)
- rejects `..` and a value containing `/` (path-traversal vectors)
- rejects the empty string
- rejects an over-length string (65+ chars)

Call `permission.ArtifactCheck.isValidPo(...)` directly (make sure the object/method is
accessible from the test — if you placed it in a companion object it is public by default).

**Verify**: `sbt -Dsbt.log.noformat=true "testOnly permission.PoFormatSpec"` → all new
tests pass.

### Step 7: Format and run the full suite

Run `sbt -Dsbt.log.noformat=true scalafmtAll`, then the full test suite.

**Verify**: `sbt -Dsbt.log.noformat=true test` → `All tests passed.`, exit 0.

## Test plan

- New file `test/permission/PoFormatSpec.scala` with the cases listed in Step 6
  (happy path for the four known PO shapes; rejection of space, `..`, `/`, empty,
  over-length).
- Structural pattern: model after `test/controllers/ModuleRelationFormatSpec.scala`.
- These are pure unit tests (no DB, no Play app) so they run under `sbt test`.
- Verification: `sbt -Dsbt.log.noformat=true test` → all pass, including the new spec.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `sbt -Dsbt.log.noformat=true compile` exits 0
- [ ] `sbt -Dsbt.log.noformat=true test` exits 0; `test/permission/PoFormatSpec.scala`
      exists and its tests pass
- [ ] `grep -n "Seq(\"latexmk\"" app/cli/LatexCompiler.scala` returns a match (string
      command form removed)
- [ ] `grep -n "startsWith(base)" app/printing/latex/WordLatexPrinter.scala` returns a match
- [ ] `grep -n "canCreateArtifact(studyProgram, po)" app/controllers/ModuleCatalogController.scala`
      returns the `uploadIntroFile` line
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 001 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts (drift since
  this plan was written).
- `sbt compile` fails to resolve the `nebulak` dependency (missing `GITHUB_TOKEN`) — this
  is an environment problem, not something to work around by editing code.
- A legitimate PO id in this system contains a character outside `[A-Za-z0-9_-]` (check
  `docs/README.md` and any PO fixtures under `test/`). If so, the regex in Step 1 would
  break real requests — report the actual character set instead of widening blindly.
- You discover an artifact endpoint that consumes `po` but does **not** go through
  `canPreviewArtifact`/`canCreateArtifact` (grep the controllers for `po:` route params) —
  it needs the same validation and the plan's choke-point assumption is incomplete.
- Any verification fails twice after a reasonable fix attempt.

## Maintenance notes

- If a new artifact endpoint is added, route its `po` through the same filter so it
  inherits validation + authorization for free — that is the reason the choke point lives
  in `ArtifactCheck` rather than per-controller.
- The `.contains(po)` authorization check assumes `artifactsCreatePermissions` /
  `artifactsPreviewPermissions` contain fully-resolved PO ids (teaching-unit contexts are
  expanded to POs upstream in `PermissionRepository`). If that resolution ever changes,
  revisit this check.
- Reviewer should scrutinize: (1) that both filters reject malformed `po` *before* the
  admin short-circuit, so even admins can't push a traversal string through; (2) that the
  `WordLatexPrinter` change still allows all legitimate PO folder names.
- Deferred: a Play `PathBindable[Po]` type would validate at routing time and remove the
  need for per-filter checks, but that is a larger refactor touching `conf/routes` and all
  `po`-consuming controllers — out of scope here.
