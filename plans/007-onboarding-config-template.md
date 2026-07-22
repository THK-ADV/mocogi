# Plan 007: Add onboarding config template + document `GITHUB_TOKEN`

> **Executor instructions**: Follow this plan step by step. Run every verification
> command and confirm the expected result before moving to the next step. If anything in
> the "STOP conditions" section occurs, stop and report — do not improvise. When done,
> update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**:
> `git diff --stat 10e852b..HEAD -- app/settings/AppSettings.scala README.md build.sbt conf/`
> If `app/settings/AppSettings.scala` changed since this plan was written, re-derive the
> required-key list from its current `load(...)` body before writing the template; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx
- **Planned at**: commit `10e852b`, 2026-07-02

## Why this matters

A fresh clone of this repo **cannot be built or run** without tribal knowledge:

1. `build.sbt` resolves the private `nebulak` parser from GitHub Packages using a
   `GITHUB_TOKEN` env var; with no token, `sbt compile` fails to resolve the dependency —
   and nothing in the repo says so.
2. `conf/application.conf` is gitignored, and `AppSettings.load` requires ~30 non-empty
   config keys (pandoc paths, mail, Keycloak, GitLab, DB, review keys). A new developer has
   no template to copy, so they cannot start the app.

`README.md` documents only test commands. This plan adds a committed, secret-free
`conf/application.conf.example` and a `.env.example`, and a README setup section, so a new
contributor (or a future CI job) can get from clone to running.

**Hard rule for the executor:** the template files must contain **placeholders only** — never
a real secret, token, password, or private URL. Do not copy values from any local
`conf/application.conf`, `conf/application-prod.conf`, or `.env` if present on disk.

## Current state

- `README.md` — only "Tests" and "SQL Formatting" sections; no setup/build steps.
- `build.sbt:29-34` — GitHub Packages credentials from `GITHUB_TOKEN` (empty-string
  fallback):

```29:34:build.sbt
    credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      "THK-ADV",
      sys.env.getOrElse("GITHUB_TOKEN", "")
    ),
```

- `build.sbt:66` — the private dependency: `"de.th-koeln.inf.adv" %% "nebulak" % "0.14"`.
- `.gitignore` ignores `conf/application.conf`, `conf/application-prod.conf`, and `.env`
  (exact paths), so `*.example` files are NOT ignored and will commit normally.
- The authoritative list of **required** config keys is `AppSettings.load`
  (`app/settings/AppSettings.scala:67-113`). Every key passed to `nonEmptyString`,
  `parseUuid`, `gitProjectIdInt`, `parseLocalDate`, and `list` there is mandatory. Additional
  keys the app reads (not via `AppSettings`) live in the existing `conf/application.conf`
  structure: `play.http.secret.key`, `play.i18n.langs`, `play.filters {...}`,
  `play.mailer {...}`, `play.evolutions`, `slick.dbs.default {...}`, `moduleKeysToReview`.
- `docker-compose.yaml` reads `${DB_USERNAME}`, `${DB_PASSWORD}`, `${DB_NAME}`,
  `${GIT_REPO_URL}`, `${GITHUB_TOKEN}` from a `.env` file.

Repo conventions: HOCON config; `AGENTS.md` values simplicity.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Verify required keys covered | `grep -c '=' conf/application.conf.example` | > 0 |
| Check no leaked secrets | `grep -nE 'glpat-|-----BEGIN' conf/application.conf.example .env.example` | no matches (exit 1) |
| Compile (optional, needs token) | `sbt -Dsbt.log.noformat=true compile` | `[success]` |

> `sbt compile` requires `GITHUB_TOKEN`. If it fails on `nebulak`, that is precisely the gap
> this plan documents — not a code error.

## Scope

**In scope** (create/modify only these):
- `conf/application.conf.example` (create)
- `.env.example` (create)
- `README.md` (modify — add a setup section)
- `build.sbt` (modify — optional Step 4 warning only)

**Out of scope** (do NOT touch):
- `conf/application.conf`, `conf/application-prod.conf`, `.env` — real config; never read
  their values into the templates and never commit them.
- `app/settings/AppSettings.scala` — read-only reference for the required keys.
- `.gitignore` — the `.example` files are already not ignored.

## Git workflow

- Branch: `advisor/007-onboarding-config-template`
- Commit message style: conventional commits, e.g.
  `docs: add config template and document build prerequisites`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Create `conf/application.conf.example`

Write the file below verbatim (placeholders only). It mirrors every key in `AppSettings.load`
plus the framework/DB keys the app needs. A developer copies it to `conf/application.conf`
and fills in real values.

```hocon
# Copy to conf/application.conf and fill in real values.
# conf/application.conf is gitignored — never commit real secrets.

play.http.secret.key = "changeme"
play.i18n.langs = ["de", "en"]
play.http.parser.maxMemoryBuffer = 1MB

keycloak {
  issuer  = "https://<keycloak-host>/auth/realms/<realm>"
  jwksUrl = ${keycloak.issuer}"/protocol/openid-connect/certs"
}

play.filters {
  disabled += play.filters.csrf.CSRFFilter
  disabled += play.filters.hosts.AllowedHostsFilter
  disabled += play.filters.csp.CSPFilter
  disabled += play.filters.headers.SecurityHeadersFilter
  enabled  += play.filters.cors.CORSFilter

  cors {
    allowedOrigins     = null
    allowedHttpMethods = null
    allowedHttpHeaders = null
    preflightMaxAge    = 1 hour
  }
}

play.mailer {
  host              = "<smtp-host>"
  timeout           = 10000
  connectiontimeout = 10000
}

mail {
  sender    = "Modulverwaltung <noreply@example.org>"
  reviewUrl = "https://<frontend-host>/module-approvals"
  editUrl   = "https://<frontend-host>/my-modules/$moduleid/general"
}

pandoc {
  texCmd                        = "pandoc --read=markdown --write=latex -r markdown-auto_identifiers"
  wordCmd                       = "pandoc -t latex -f docx --extract-media=. --wrap=none"
  moduleCatalogOutputFolderPath = "output/catalogs"
  examListOutputFolderPath      = "output/examlist"
  mcIntroPath                   = "mc/intro"
  mcAssetsPath                  = "mc/assets"
}

git {
  # git.token must be a valid UUID (the GitLab webhook secret token).
  token                 = "00000000-0000-0000-0000-000000000000"
  # git.accessToken is a GitLab personal access token (api scope). Placeholder only.
  accessToken           = "<gitlab-access-token>"
  baseUrl               = "https://<gitlab-host>/api/v4"
  repoUrl               = "https://<gitlab-host>/<group>/<project>"
  projectId             = "0"
  mainBranch            = "main"
  draftBranch           = "preview"
  modulesFolder         = "modules"
  coreFolder            = "core"
  moduleCatalogsFolder  = "catalogs"
  moduleCompanionFolder = "module-companions"
  autoApprovedLabel     = "auto approved"
  reviewRequiredLabel   = "review required"
  fastForwardLabel      = "fast forward"
  bigBangLabel          = "big bang"
  moduleCatalogLabel    = "module catalog"
  defaultEmail          = "mocogi@example.org"
  defaultUser           = "mocogi"
  localGitFolderPath    = "/absolute/path/to/local/modules/checkout"
  historySince          = "2025-01-01"
}

play.temporaryFile.dir = "tmp"

play.evolutions {
  db.default.autoApply = true
}

slick.dbs {
  default {
    profile = "slick.jdbc.PostgresProfile$"
    db {
      driver       = "org.postgresql.Driver"
      url          = "jdbc:postgresql://localhost:5432/mocogi"
      databaseName = "mocogi"
      user         = "postgres"
      password     = ""
    }
  }
}

# Keep in sync with ModuleDraftReviewController.keys()
moduleKeysToReview {
  pav = [
    "metadata.assessmentMethods.mandatory",
    "metadata.title",
    "metadata.ects",
    "metadata.moduleManagement",
    "metadata.examiner.first",
    "metadata.examiner.second",
    "metadata.examPhases",
    "metadata.attendanceRequirement",
    "metadata.assessmentPrerequisite"
  ]
}
```

**Verify**: for each mandatory key, confirm it is present in the template. Run:
```
for k in play.temporaryFile.dir pandoc.wordCmd pandoc.texCmd pandoc.mcIntroPath \
  pandoc.mcAssetsPath pandoc.examListOutputFolderPath pandoc.moduleCatalogOutputFolderPath \
  mail.sender mail.reviewUrl mail.editUrl keycloak.jwksUrl keycloak.issuer git.repoUrl \
  git.token git.localGitFolderPath git.accessToken git.baseUrl git.projectId git.mainBranch \
  git.draftBranch git.modulesFolder git.coreFolder git.moduleCatalogsFolder \
  git.moduleCompanionFolder git.autoApprovedLabel git.reviewRequiredLabel git.fastForwardLabel \
  git.bigBangLabel git.moduleCatalogLabel git.defaultEmail git.defaultUser git.historySince; do
  key=$(echo "$k" | sed 's/.*\.//')
  grep -q "$key" conf/application.conf.example || echo "MISSING: $k"
done
```
→ prints nothing (every mandatory key's leaf name appears).

### Step 2: Create `.env.example`

Write:

```dotenv
# Copy to .env and fill in real values. .env is gitignored.

# GitHub personal access token with read:packages scope — required to resolve the
# private `nebulak` dependency from GitHub Packages when building.
GITHUB_TOKEN=

# Postgres container credentials (used by docker-compose.yaml)
DB_USERNAME=postgres
DB_PASSWORD=changeme
DB_NAME=mocogi

# URL of the GitLab module repository cloned into the backend image
GIT_REPO_URL=https://<gitlab-host>/<group>/<project>.git
```

**Verify**: `grep -nE 'glpat-|-----BEGIN|ghp_' .env.example` → no matches (exit 1).

### Step 3: Add a setup section to `README.md`

Insert a `## Setup` section **above** the existing `## Tests` section:

```markdown
## Setup

### Prerequisites

- JDK 21 and sbt 1.10.x
- PostgreSQL 17 (or run the bundled `docker-compose.yaml`)
- pandoc + a LaTeX toolchain (`latexmk`, `xelatex`) on `PATH` for catalog/exam-list PDFs
- A **`GITHUB_TOKEN`** environment variable with `read:packages` scope. The build resolves
  the private `nebulak` parser from GitHub Packages; without this token `sbt compile` fails
  to resolve `de.th-koeln.inf.adv:nebulak`.

### Configuration

1. `cp conf/application.conf.example conf/application.conf` and fill in real values
   (Keycloak, GitLab, mail, DB). `conf/application.conf` is gitignored.
2. `cp .env.example .env` and set `GITHUB_TOKEN`, DB credentials, and `GIT_REPO_URL`
   (used by `docker-compose.yaml`).
3. `export GITHUB_TOKEN=...` (or ensure it is in your shell env) before building.

### Run

- `sbt run` starts the app on port 9000 (evolutions auto-apply against the configured DB).
- Or `docker compose up` to run Postgres + backend + core + frontend.
```

**Verify**: `grep -n "GITHUB_TOKEN" README.md` → returns a match;
`grep -n "## Setup" README.md` → returns a match.

### Step 4 (optional): Warn in `build.sbt` when `GITHUB_TOKEN` is empty

Only do this if it is trivial and does not break token-less tasks (formatting, etc.). Add a
non-fatal warning at project load so a missing token surfaces early. Do NOT throw — that
would break `sbt scalafmtAll` and other tasks that don't need the token. Example (place near
the `credentials` line):

```scala
    // Surface the common onboarding failure early (non-fatal).
    credentials += {
      if (sys.env.get("GITHUB_TOKEN").forall(_.isEmpty))
        sLog.value.warn("GITHUB_TOKEN is not set; resolving the private `nebulak` dependency will fail.")
      Credentials(
        "GitHub Package Registry",
        "maven.pkg.github.com",
        "THK-ADV",
        sys.env.getOrElse("GITHUB_TOKEN", "")
      )
    },
```

If this does not compile cleanly on the first try, revert it — the warning is a nicety, not a
requirement of this plan.

**Verify**: `sbt -Dsbt.log.noformat=true compile` (with a token) → `[success]`, or if no
token is available, confirm the warning line appears in sbt output and STOP per conditions.

### Step 5: Confirm the templates are tracked and secret-free

**Verify**:
- `git status --porcelain conf/application.conf.example .env.example` → both show as added.
- `git check-ignore conf/application.conf.example .env.example` → prints nothing (not ignored).
- `grep -REn 'glpat-|ghp_|-----BEGIN|:[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-' conf/application.conf.example .env.example`
  → no matches (no real tokens, no real v4 UUID other than the all-zero placeholder).

## Test plan

- This is a documentation/onboarding change; there is no code under test. Verification is the
  grep-based key-coverage and secret-absence checks above.
- If a `GITHUB_TOKEN` is available in the environment, a full `sbt compile` is the strongest
  end-to-end check that the documented prerequisite is accurate.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `conf/application.conf.example` exists and the Step 1 key-coverage loop prints nothing
- [ ] `.env.example` exists and documents `GITHUB_TOKEN`, DB creds, `GIT_REPO_URL`
- [ ] `README.md` contains a `## Setup` section mentioning `GITHUB_TOKEN`
- [ ] `git check-ignore conf/application.conf.example .env.example` prints nothing
- [ ] No real secret/token/UUID appears in either template (Step 5 grep returns no matches)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 007 updated

## STOP conditions

Stop and report back (do not improvise) if:

- `app/settings/AppSettings.scala` has drifted and now requires keys not in the template —
  add them (re-derive from `load(...)`) and note it, or STOP if the change is large.
- You find a real secret value in a local `conf/application*.conf` or `.env` and are tempted
  to copy it — do NOT; use placeholders. If a real secret has ever been committed (check
  `git log -p -- conf/application.conf`), STOP and report it as a rotation-required finding.
- The optional Step 4 build.sbt change breaks any sbt task — revert it and continue.

## Maintenance notes

- Keep `conf/application.conf.example` in sync with `AppSettings.load`: when a new required
  key is added there, add it to the template in the same PR. Consider a tiny CI check that
  greps the example for each `nonEmptyString(configuration, "...")` key.
- The `moduleKeysToReview.pav` list must stay aligned with
  `ModuleDraftReviewController.keys()` (noted in the existing config).
- Reviewer should confirm the template has no real credentials and that the README setup
  steps actually produce a runnable app.
