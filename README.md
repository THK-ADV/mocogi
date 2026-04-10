# mocogi

## Tests

- **Default (CI / quick local):** `sbt test` — unit and parser tests only; no Postgres snapshot suites.
- **DB snapshot tests:** need a restored DB (e.g. `./scripts/sync-test-db-from-prod.sh`), then `sbt it:test`. Expected files live under `test/resources/database/expected/` (gitignored); refresh with `UPDATE_SNAPSHOTS=1 sbt it:test` after intentional SQL/output changes. Single suite: `sbt "it:testOnly database.YourSpec"`.

## SQL Formatting

Use pg_format to format SQL files:

```bash
pg_format -i your_file.psql
```
