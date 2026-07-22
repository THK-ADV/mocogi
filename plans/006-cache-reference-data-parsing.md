# Plan 006: Cache reference data in `MetadataParsingService`

> **Executor instructions**: Follow this plan step by step. Run every verification
> command and confirm the expected result before moving to the next step. If anything in
> the "STOP conditions" section occurs, stop and report — do not improvise. When done,
> update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**:
> `git diff --stat 10e852b..HEAD -- app/service/pipeline/MetadataParsingService.scala app/git/publisher/CoreDataPublisher.scala`
> If any in-scope file changed since this plan was written, compare the "Current state"
> excerpts against the live code before proceeding; on a mismatch, treat it as a STOP
> condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: MED
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `10e852b`, 2026-07-02

## Why this matters

`MetadataParsingService.parser` reads **nine reference tables** (locations, languages,
statuses, assessment methods, module types, seasons, identities/persons, POs,
specializations) from the database every time a module is parsed or validated. This
reference data is almost entirely static between core-data syncs, yet it is re-fetched on
every `PUT /modules/parse`, every draft save, and once per webhook merge batch. Caching the
assembled snapshot removes those repeated reads.

**Why this is MED-risk, not a trivial cache:** the loaded data (persons, POs,
specializations) is exactly what the metadata parser validates module references against. A
stale cache could reject a module that references a person or PO added seconds earlier. This
plan therefore pairs a TTL with **explicit cache invalidation when core data is
(re)published** (`CoreDataPublisher`), so a fresh sync always clears the cache — the TTL is
only a safety net.

## Current state

`app/service/pipeline/MetadataParsingService.scala:36-68` — the reference-data load runs on
every `parser` invocation:

```36:68:app/service/pipeline/MetadataParsingService.scala
  private def parser = {
    val locations         = locationService.all()
    val languages         = languageService.all()
    val status            = statusService.all()
    val assessmentMethods = assessmentMethodRepo.all()
    val moduleTypes       = moduleTypeService.all()
    val seasons           = seasonService.all()
    val persons           = personService.all()
    val pos               = poService.all()
    val specializations   = specializationService.all()
    for {
      locations         <- locations
      languages         <- languages
      status            <- status
      assessmentMethods <- assessmentMethods
      moduleTypes       <- moduleTypes
      seasons           <- seasons
      persons           <- persons
      pos               <- pos
      specializations   <- specializations
    } yield metadataParser
      .parser(
        locations,
        languages,
        status,
        assessmentMethods,
        moduleTypes,
        seasons,
        persons,
        pos,
        specializations
      )
  }
```

`parser` is called by `parseMany` (once per batch) and `parse` (once per call) in the same
file. Its result type is `Future[parser.Parser[ParsedMetadata]]`. `metadataParser.parser`
takes the nine collections as (explicitly-applied) implicit parameters.

**Exemplar** — this repo already caches with `AsyncCacheApi` via `getOrElseUpdate`
(`app/auth/JwtAuthorization.scala:35-36,86-92`):

```35:36:app/auth/JwtAuthorization.scala
  private val cacheKey = "keycloak-jwks"
  private val cacheTtl = 1.hour
```

```86:92:app/auth/JwtAuthorization.scala
  private def getJwks(): Future[JsValue] =
    cache.getOrElseUpdate(cacheKey, cacheTtl)(fetchJwks())

  /** Fetches the JWKS from Keycloak's well-known endpoint. */
  private def fetchJwks(): Future[JsValue] =
    ws.url(jwksUrl).get().map(_.json)
```

The invalidation point — `CoreDataPublisher` refreshes all core data then the views
(`app/git/publisher/CoreDataPublisher.scala:64-68`):

```64:68:app/git/publisher/CoreDataPublisher.scala
      val res = for {
        _ <- updates
        _ <- studyProgramViewRepository.refreshView()
        _ <- moduleViewRepository.refreshView()
      } yield ()
```

`CoreDataPublisher` is a Guice-managed actor (`bindActor[CoreDataPublisher]` in
`app/Module.scala`), so adding a constructor-injected `AsyncCacheApi` is supported.

Repo conventions: expression-oriented Scala; `AGENTS.md` prefers injecting the smallest
dependency and simple bindings.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `sbt -Dsbt.log.noformat=true compile` | `[success]`, exit 0 |
| Unit tests | `sbt -Dsbt.log.noformat=true test` | `All tests passed.`, exit 0 |
| Format | `sbt -Dsbt.log.noformat=true scalafmtAll` | exit 0 |

> Building requires `GITHUB_TOKEN` (read:packages). A `nebulak` resolution failure is the
> missing-token problem — STOP and report (see plan 007).

## Scope

**In scope** (the only files you should modify):
- `app/service/pipeline/MetadataParsingService.scala`
- `app/git/publisher/CoreDataPublisher.scala`

**Out of scope** (do NOT touch):
- `app/service/pipeline/MetadataPipeline.scala` — its `allModules()` call is a separate
  concern (module cores, not reference data); leave it.
- `app/parsing/metadata/MetadataCompositeParser.scala` — the pure `parser(...)` builder.
- The `personService`/`poService`/etc. services themselves.

## Git workflow

- Branch: `advisor/006-cache-reference-data`
- Commit message style: conventional commits, e.g.
  `perf: cache reference data used by metadata parser`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Cache the reference-data load behind a TTL

In `app/service/pipeline/MetadataParsingService.scala`:

1. Add imports:
   ```scala
   import scala.concurrent.duration.*
   import play.api.cache.AsyncCacheApi
   ```
2. Inject `AsyncCacheApi` into the constructor (add a parameter, e.g. after
   `specializationService`):
   ```scala
       private val specializationService: SpecializationService,
       private val cache: AsyncCacheApi,
       private implicit val ctx: ExecutionContext
   ```
3. Add a companion object holding the shared cache key (public so `CoreDataPublisher` can
   reference the same key), plus a TTL:
   ```scala
   object MetadataParsingService {
     val ReferenceDataCacheKey = "metadata-reference-data"
   }
   ```
   and inside the class a `private val referenceDataTtl = 1.hour` (safety-net TTL; the
   cache is invalidated on every core-data publish in Step 2, so a long TTL is safe).
4. Cache the nine-collection load as a single value and derive `parser` from it. Cache a
   tuple whose type is inferred from the yield (this avoids naming the nine element types):

   ```scala
   private def referenceData =
     cache.getOrElseUpdate(MetadataParsingService.ReferenceDataCacheKey, referenceDataTtl) {
       val locations         = locationService.all()
       val languages         = languageService.all()
       val status            = statusService.all()
       val assessmentMethods = assessmentMethodRepo.all()
       val moduleTypes       = moduleTypeService.all()
       val seasons           = seasonService.all()
       val persons           = personService.all()
       val pos               = poService.all()
       val specializations   = specializationService.all()
       for {
         locations         <- locations
         languages         <- languages
         status            <- status
         assessmentMethods <- assessmentMethods
         moduleTypes       <- moduleTypes
         seasons           <- seasons
         persons           <- persons
         pos               <- pos
         specializations   <- specializations
       } yield (locations, languages, status, assessmentMethods, moduleTypes, seasons, persons, pos, specializations)
     }

   private def parser =
     referenceData.map { rd =>
       metadataParser.parser(rd._1, rd._2, rd._3, rd._4, rd._5, rd._6, rd._7, rd._8, rd._9)
     }
   ```

   Keep `parseMany` and `parse` exactly as they are — they still call `parser.map { p => ... }`.

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`. If `getOrElseUpdate`
complains about a missing `ClassTag` for the tuple, see STOP conditions.

### Step 2: Invalidate the cache when core data is published

In `app/git/publisher/CoreDataPublisher.scala`:

1. Add import `import play.api.cache.AsyncCacheApi` and
   `import service.pipeline.MetadataParsingService`.
2. Inject `AsyncCacheApi` into the actor constructor (add a parameter, e.g. before
   `private implicit val ctx`):
   ```scala
       private val cache: AsyncCacheApi,
       private implicit val ctx: ExecutionContext
   ```
3. Clear the reference-data cache as part of the post-sync refresh, so the next parse
   reloads fresh data:
   ```scala
       val res = for {
         _ <- updates
         _ <- studyProgramViewRepository.refreshView()
         _ <- moduleViewRepository.refreshView()
         _ <- cache.remove(MetadataParsingService.ReferenceDataCacheKey)
       } yield ()
   ```

**Verify**: `sbt -Dsbt.log.noformat=true compile` → `[success]`. Then
`grep -n "ReferenceDataCacheKey" app/git/publisher/CoreDataPublisher.scala` → returns a match.

### Step 3: Format and run the full suite

`sbt -Dsbt.log.noformat=true scalafmtAll` then `sbt -Dsbt.log.noformat=true test`.

**Verify**: `sbt -Dsbt.log.noformat=true test` → `All tests passed.`, exit 0.

## Test plan

- No new unit test: the change is a caching/invalidation behavior against injected services
  and Play's cache; there is no existing harness that exercises `MetadataParsingService`
  end-to-end without a full application context, and adding one is out of scope.
- Regression safety: `sbt test` stays green (parse/validate behavior is unchanged on a warm
  or cold cache), and the injected `AsyncCacheApi` is the same module already used by
  `JwtAuthorization`, so DI wiring is proven.
- Manual sanity (optional, if running the app locally): parse the same module twice and
  confirm the nine reference queries appear in DB logs only on the first call within the TTL;
  publish a core-data change and confirm the next parse reloads.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `sbt -Dsbt.log.noformat=true compile` exits 0
- [ ] `sbt -Dsbt.log.noformat=true test` exits 0 (no regressions)
- [ ] `grep -n "getOrElseUpdate" app/service/pipeline/MetadataParsingService.scala` returns a match
- [ ] `grep -n "cache.remove(MetadataParsingService.ReferenceDataCacheKey)" app/git/publisher/CoreDataPublisher.scala`
      returns a match
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 006 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The "Current state" excerpts don't match the live code (drift).
- `sbt compile` fails to resolve `nebulak` (missing `GITHUB_TOKEN`) — environment issue.
- `getOrElseUpdate` requires a `ClassTag` the tuple can't provide and it won't compile after
  one attempt: fall back to a named `private case class ReferenceData(...)` whose field types
  are copied from the parameters of `metadataParser.parser` in
  `app/parsing/metadata/MetadataCompositeParser.scala:27-37` (nine `Seq[...]` types), and
  cache/return that instead of a tuple. Report which approach you used.
- Injecting `AsyncCacheApi` into `CoreDataPublisher` breaks its Guice actor construction
  (test suite fails at wiring) — report the error rather than removing the invalidation.
- Any verification fails twice after a reasonable fix attempt.

## Maintenance notes

- **The invalidation in Step 2 is load-bearing for correctness.** If a new code path mutates
  reference data (persons, POs, statuses, etc.) outside `CoreDataPublisher`, it must also
  clear `MetadataParsingService.ReferenceDataCacheKey`, or parsing will use stale data until
  the TTL expires. Document any such new path.
- The TTL is a safety net; if reference data is ever mutated by a path that can't easily
  evict the cache, shorten the TTL.
- Reviewer should confirm: (1) the cached tuple is passed to `metadataParser.parser` in the
  same order as the original call; (2) the invalidation runs on successful publish; (3) no
  behavior change for a cold cache.
- Deferred: `MetadataPipeline.allModules()` also re-reads module cores on every validate —
  a separate (and more staleness-sensitive) caching question, intentionally not addressed here.
