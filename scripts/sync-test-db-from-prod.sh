#!/usr/bin/env bash
# Local Postgres (127.0.0.1:5432, user postgres): clone mocogi -> mocogi_test.
#
# Plain SQL stream (not custom pg_dump -Fc) so we can strip SETs from newer pg_dump that your
# server rejects (e.g. transaction_timeout on PostgreSQL < 17).
#
# Trust/peer auth; for password auth use ~/.pgpass.
set -euo pipefail

echo "Recreating mocogi_test"
dropdb -h 127.0.0.1 -p 5432 -U postgres --if-exists mocogi_test
createdb -h 127.0.0.1 -p 5432 -U postgres mocogi_test

echo "Dumping mocogi and restoring into mocogi_test"
pg_dump -h 127.0.0.1 -p 5432 -U postgres -Fp --no-owner --no-acl mocogi \
  | sed -E '/^SET (transaction_timeout|idle_in_transaction_session_timeout) /d' \
  | psql -h 127.0.0.1 -p 5432 -U postgres -v ON_ERROR_STOP=1 -d mocogi_test -f -

echo "Done."
