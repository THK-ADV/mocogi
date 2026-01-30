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

-- =====================
-- CLOSED BUILDING - applies to both teaching units
-- =====================
INSERT INTO schedule.semester_plan(id, start, "end", type, teaching_unit, semester_index, phase)
  VALUES (gen_random_uuid(), '2025-12-22', '2026-01-03', 'closed_building', NULL, NULL, NULL);

-- =====================
-- INF
-- =====================
-- Lectures
INSERT INTO schedule.semester_plan(id, start, "end", type, teaching_unit, semester_index, phase)
VALUES
  (gen_random_uuid(), '2025-10-06', '2025-11-22', 'lecture', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2025-12-01', '2025-12-20', 'lecture', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-01-12', '2026-02-01', 'lecture', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-04-20', '2026-05-16', 'lecture', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-05-25', '2026-07-25', 'lecture', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL);

-- Exams
INSERT INTO schedule.semester_plan(id, start, "end", type, teaching_unit, semester_index, phase)
VALUES
  (gen_random_uuid(), '2025-09-19', '2025-10-03', 'exam', '24b1ba0f-04c5-49ab-b652-b547a120662c', ARRAY[3, 5], NULL),
(gen_random_uuid(), '2026-02-02', '2026-02-07', 'exam', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-04-13', '2026-04-18', 'exam', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-07-27', '2026-08-01', 'exam', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL);

-- Block (all semesters)
INSERT INTO schedule.semester_plan(id, start, "end", type, teaching_unit, semester_index, phase)
VALUES
  (gen_random_uuid(), '2025-11-24', '2025-11-29', 'block', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-01-05', '2026-01-10', 'block', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-04-06', '2026-04-11', 'block', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL),
(gen_random_uuid(), '2026-05-18', '2026-05-23', 'block', '24b1ba0f-04c5-49ab-b652-b547a120662c', NULL, NULL);

-- =====================
-- ING
-- =====================
-- Lectures
INSERT INTO schedule.semester_plan(id, start, "end", type, teaching_unit, semester_index, phase)
VALUES
  (gen_random_uuid(), '2025-10-06', '2025-11-01', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2025-11-17', '2025-11-22', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2025-12-08', '2025-12-20', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-01-19', '2026-02-01', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2025-11-10', '2025-11-15', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2025-12-01', '2025-12-06', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-01-12', '2026-01-17', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2025-11-03', '2025-11-08', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2025-11-24', '2025-11-29', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-01-05', '2026-01-10', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-04-20', '2026-05-09', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-05-25', '2026-06-06', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-06-22', '2026-06-27', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-07-13', '2026-07-25', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-05-11', '2026-05-16', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-06-08', '2026-06-13', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-06-29', '2026-07-04', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-05-18', '2026-05-23', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-06-25', '2026-06-20', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-07-06', '2026-07-11', 'lecture', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL);

-- Exams
INSERT INTO schedule.semester_plan(id, start, "end", type, teaching_unit, semester_index, phase)
VALUES
  (gen_random_uuid(), '2025-09-19', '2025-10-03', 'exam', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-02-02', '2026-02-07', 'exam', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-07-27', '2026-08-08', 'exam', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-04-13', '2026-04-18', 'exam', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2, 3, 4, 5, 6, 7], NULL);

-- Block
INSERT INTO schedule.semester_plan(id, start, "end", type, teaching_unit, semester_index, phase)
VALUES
  (gen_random_uuid(), '2025-11-03', '2025-11-08', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2025-11-24', '2025-11-29', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-01-05', '2026-01-10', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2025-11-10', '2025-11-15', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2025-12-01', '2025-12-06', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-01-12', '2026-01-17', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-05-18', '2026-05-23', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-06-15', '2026-06-20', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-07-06', '2026-07-11', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1, 3, 4, 5, 6, 7], NULL),
(gen_random_uuid(), '2026-05-11', '2026-05-16', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-06-08', '2026-06-13', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-06-29', '2026-07-04', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[2], NULL),
(gen_random_uuid(), '2026-04-06', '2026-04-11', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', NULL, NULL),
(gen_random_uuid(), '2026-04-13', '2026-04-18', 'block', 'bfa12ba1-e990-4b08-8815-16a7f0a7569a', ARRAY[1], NULL);

CREATE TABLE schedule.room(
  "id" uuid PRIMARY KEY,
  "label" text NOT NULL,
  "abbrev" text NOT NULL,
  "type" text NOT NULL,
  "capacity" int NOT NULL
);

CREATE TABLE schedule.course(
  "id" uuid PRIMARY KEY,
  "module" uuid NOT NULL, -- don't use a foreign key to support modules in preview
  "type" text NOT NULL
);

CREATE TABLE schedule.schedule_entry(
  "id" uuid,
  "course" uuid NOT NULL,
  "start" timestamp without time zone NOT NULL,
  "end" timestamp without time zone NOT NULL,
  "room" uuid NOT NULL,
  "props" jsonb NOT NULL,
  PRIMARY KEY (id, START),
  FOREIGN KEY (course) REFERENCES schedule.course(id),
  FOREIGN KEY (room) REFERENCES schedule.room(id)
)
PARTITION BY RANGE (START);

-- NOTE: PostgreSQL range partitions use exclusive upper bounds
CREATE TABLE schedule.schedule_entry_wise_2025 PARTITION OF schedule.schedule_entry
FOR VALUES FROM ('2025-09-01') TO ('2026-03-01');

CREATE TABLE schedule.schedule_entry_sose_2026 PARTITION OF schedule.schedule_entry
FOR VALUES FROM ('2026-03-01') TO ('2026-09-01');

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

DROP TABLE IF EXISTS schedule.course;

DROP TABLE IF EXISTS schedule.room;

DROP TABLE IF EXISTS schedule.module_teaching_unit;

DROP TABLE IF EXISTS schedule.semester_plan;

DROP TABLE IF EXISTS core.teaching_unit;

