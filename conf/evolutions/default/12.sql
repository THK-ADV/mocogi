-- !Ups
CREATE TABLE schedule.plan_draft(
  "id" uuid PRIMARY KEY,
  "kind" text NOT NULL CHECK (kind IN ('schedule', 'exam')),
  "semester" text NOT NULL CHECK (semester ~ '^(wise|sose)_[0-9]{4}$'),
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "published_at" timestamp NULL
);

CREATE UNIQUE INDEX idx_plan_draft_one_active ON schedule.plan_draft(kind, semester)
WHERE
  published_at IS NULL;

CREATE TABLE schedule.schedule_entry_draft(
  "id" uuid PRIMARY KEY,
  "plan_draft" uuid NOT NULL REFERENCES schedule.plan_draft(id) ON DELETE CASCADE,
  "series_id" uuid NOT NULL,
  "module" uuid NOT NULL REFERENCES modules.module(id),
  "course_type" text NOT NULL,
  "start" timestamptz NOT NULL,
  "end" timestamptz NOT NULL,
  "rooms" uuid[] NOT NULL,
  "lecturer" text[] NOT NULL,
  "po" jsonb NOT NULL,
  CHECK ("end" > "start")
);

CREATE INDEX idx_schedule_entry_draft_plan_draft ON schedule.schedule_entry_draft(plan_draft);

ALTER TABLE schedule.schedule_entry
  ADD COLUMN "series_id" uuid NOT NULL DEFAULT gen_random_uuid(),
  ADD COLUMN "source_plan_draft" uuid NULL REFERENCES schedule.plan_draft(id) ON DELETE SET NULL,
  ADD COLUMN "source_schedule_entry_draft" uuid NULL REFERENCES schedule.schedule_entry_draft(id) ON DELETE SET NULL,
  ADD COLUMN "lecturer" text[] NULL,
  ADD COLUMN "po" jsonb NULL;

UPDATE
  schedule.schedule_entry se
SET
  lecturer = ARRAY (
    SELECT
      lecturer_id
    FROM
      jsonb_array_elements_text(se.props -> 'lecturer')
      WITH ORDINALITY AS lecturer(lecturer_id, position)
    ORDER BY
      position), po = se.props -> 'po';

ALTER TABLE schedule.schedule_entry
  ALTER COLUMN "lecturer" SET NOT NULL,
  ALTER COLUMN "po" SET NOT NULL,
  DROP COLUMN "props";

CREATE INDEX idx_schedule_entry_series_id ON schedule.schedule_entry(series_id);

-- !Downs
DROP INDEX IF EXISTS schedule.idx_schedule_entry_series_id;

ALTER TABLE schedule.schedule_entry
  ADD COLUMN "props" jsonb NULL;

UPDATE
  schedule.schedule_entry
SET
  props = jsonb_build_object('lecturer', to_jsonb(lecturer), 'po', po);

ALTER TABLE schedule.schedule_entry
  ALTER COLUMN "props" SET NOT NULL,
  DROP COLUMN "lecturer",
  DROP COLUMN "po",
  DROP COLUMN IF EXISTS "source_schedule_entry_draft",
  DROP COLUMN IF EXISTS "source_plan_draft",
  DROP COLUMN IF EXISTS "series_id";

DROP INDEX IF EXISTS schedule.idx_schedule_entry_draft_plan_draft;

DROP TABLE IF EXISTS schedule.schedule_entry_draft;

DROP INDEX IF EXISTS schedule.idx_plan_draft_one_active;

DROP TABLE IF EXISTS schedule.plan_draft;

