-- !Ups
CREATE TABLE core.teaching_unit(
  "id" uuid PRIMARY KEY,
  "faculty" text NOT NULL,
  "label" text NOT NULL,
  "abbrev" text NOT NULL
);

INSERT INTO core.teaching_unit
VALUES
  ('24b1ba0f-04c5-49ab-b652-b547a120662c', 'f10', 'Informatik', 'INF'),
('bfa12ba1-e990-4b08-8815-16a7f0a7569a', 'f10', 'Ingenieurwesen', 'ING');

CREATE TABLE schedule.semester_plan(
  "id" uuid PRIMARY KEY,
  "start" date,
  "end" date,
  "type" text NOT NULL,
  "teaching_unit" uuid NULL,
  "semester_index" integer[] NULL,
  "phase" text NULL,
  FOREIGN KEY (teaching_unit) REFERENCES core.teaching_unit(id)
);

CREATE TABLE schedule.room(
  "id" uuid PRIMARY KEY,
  "label" text NOT NULL,
  "abbrev" text NOT NULL,
  "type" text NOT NULL,
  "capacity" int NOT NULL
);

ALTER DATABASE mocogi SET timezone = 'Europe/Berlin';

CREATE TABLE schedule.schedule_entry(
  "id" uuid,
  "module" uuid NOT NULL,
  "course_type" text NOT NULL,
  "start" timestamptz NOT NULL,
  "end" timestamptz NOT NULL,
  "rooms" uuid[] NOT NULL,
  "props" jsonb NOT NULL,
  PRIMARY KEY (id, START),
  FOREIGN KEY (module) REFERENCES modules.module(id)
)
PARTITION BY RANGE (START);

-- NOTE: PostgreSQL range partitions use exclusive upper bounds
CREATE TABLE schedule.schedule_entry_wise_2025 PARTITION OF schedule.schedule_entry
FOR VALUES FROM ('2025-09-01 00:00:00+02') -- September = CEST
TO ('2026-03-01 00:00:00+01');

-- März = CET
CREATE TABLE schedule.schedule_entry_sose_2026 PARTITION OF schedule.schedule_entry
FOR VALUES FROM ('2026-03-01 00:00:00+01') -- März = CET
TO ('2026-09-01 00:00:00+02');

-- September = CEST
-- NOTE: Only add if EXPLAIN ANALYZE shows sequential scans being slow
CREATE INDEX idx_schedule_entry_start_end ON schedule.schedule_entry("start", "end");

CREATE TABLE schedule.module_teaching_unit(
  "module" uuid PRIMARY KEY,
  "teaching_units" uuid[] NOT NULL
);

-- !Downs
DELETE FROM schedule.semester_plan;

DELETE FROM core.teaching_unit;

DROP INDEX IF EXISTS schedule.idx_schedule_entry_start_end;

DROP TABLE IF EXISTS schedule.schedule_entry_wise_2025;

DROP TABLE IF EXISTS schedule.schedule_entry_sose_2026;

DROP TABLE IF EXISTS schedule.schedule_entry;

DROP TABLE IF EXISTS schedule.room;

DROP TABLE IF EXISTS schedule.module_teaching_unit;

DROP TABLE IF EXISTS schedule.semester_plan;

DROP TABLE IF EXISTS core.teaching_unit;

