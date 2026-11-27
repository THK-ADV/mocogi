- Use the least code necessary to fully achieve a goal
- Prefer simple, concise implementations; avoid unnecessary abstractions
- Do not write tests unless explicitly requested
- Reach for slim and performant code in space and time. Consider appropriate data structures and algorithms
- The development language is english, although some words are kept in german for domain knowledge
- Do not wirte docs unless explicitly stated
- Avoid pointless transformations between wrapper types (e.g. `Try` -> `Option` -> `Either`, or `Option` -> `throw`) just to check a condition or rethrow. Stay in one effect type, use `try`/`catch` or `require` directly, and let code that may fail fast (e.g. config loading at startup) throw instead of wrapping and unwrapping
- Write comments as Scaladoc. State what the code does and, only if not evident, why. Keep them short and precise. Add `@param`/`@return` only when names and types do not convey the semantics
- When the Scala standard library falls short for monadic code, use cats instead of hand-rolled workarounds (e.g. `Either.raiseUnless` instead of `Either.cond(_, (), _)`, `Either.catchOnly` instead of `Try(...).toEither.left.map(...)`)
- In Slick queries, do the work in the database: filter, project and transform there instead of loading rows and processing them in Scala. For Postgres features (JSON, arrays, etc.) use the slick-pg operators from `MyPostgresProfile.MyAPI` (e.g. `+>>` instead of parsing a `JsValue` in Scala)

## Postgres reviews

When asked for a Postgres review, review all database operations in scope (plain SQL, functions in `conf/sql/functions.sql`, evolutions and Slick queries), measure them and optimize for performance and readability:

- Measure before and after with `EXPLAIN (ANALYZE, BUFFERS)` on realistic worst-case data, e.g. a full module sync or every PO. Stage that data inside `BEGIN … ROLLBACK`, so nothing persists, and report the timings
- Prove that a rewrite returns exactly the same result as the original by comparing both outputs on the same data. Make orderings deterministic with a unique tiebreaker (e.g. `ORDER BY title, id`)
- Look for work that repeats per row: correlated subqueries evaluated per candidate, JSON path searches (especially `$.**`) on cross joins. Start from the smallest set and aggregate once with a CTE and a join instead
- Prefer `NOT EXISTS` over `NOT IN (subquery)`, which breaks on `NULL` and cannot become an anti-join
- Add an index only if a measurement on realistic sizes shows a benefit; sequential scans over a few thousand rows are fine and indexes cost on every write
- Write only rows that actually change
- Keep SQL readable: distinct aliases that say what a relation is (no reused `c`/`r`), named CTEs, one `jsonb_build_object` key per line, and a short comment for non-obvious semantics (e.g. existential `==` in JSON paths)
- Put statements with real logic into `functions.sql` and call them from the repository; their plain-SQL call adds no measurable overhead. New or changed functions require reapplying `functions.sql` on deployment
