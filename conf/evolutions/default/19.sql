-- !Ups
CREATE SCHEMA IF NOT EXISTS exams;

-- exam of one or more modules with own exams (neither generic nor child modules), names are not unique
CREATE TABLE exams.exam(
  id uuid PRIMARY KEY,
  name text NOT NULL
);

CREATE TABLE exams.exam_module(
  exam uuid NOT NULL REFERENCES exams.exam(id) ON DELETE CASCADE,
  module uuid NOT NULL REFERENCES modules.module(id) ON DELETE CASCADE,
  PRIMARY KEY (exam, module)
);

-- bootstrapping: one exam per module with own exams, named after the module
WITH OWNER AS (
  SELECT
    gen_random_uuid() AS exam,
    module.id AS module,
    module.title
  FROM
    modules.module module
  WHERE
    module.module_type <> 'generic_module'
    AND NOT EXISTS (
      SELECT
      FROM
        modules.module_relation relation
      WHERE
        relation.child = module.id)
),
created AS (
INSERT INTO exams.exam(id, name)
  SELECT
    exam,
    title
  FROM
    OWNER)
INSERT INTO exams.exam_module(exam, module)
SELECT
  exam,
  module
FROM
  OWNER;

-- exam lists are identified by id and kept per po and semester
ALTER TABLE modules.exam_list SET SCHEMA exams;

ALTER TABLE exams.exam_list
  ADD COLUMN id uuid NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE exams.exam_list
  DROP CONSTRAINT exam_list_pkey;

ALTER TABLE exams.exam_list
  ADD PRIMARY KEY (id);

ALTER TABLE exams.exam_list
  ADD UNIQUE (po, semester);

-- one row per module of the published exam list, the module data is copied at publication
-- module and exams have no foreign keys, so published entries outlive them
-- exams: [{ exam, name, registrationPeriods: { phase: { start, end } } }] with the exam name at publication
CREATE TABLE exams.exam_list_entry(
  id uuid PRIMARY KEY,
  exam_list uuid NOT NULL REFERENCES exams.exam_list(id) ON DELETE CASCADE,
  module uuid NOT NULL,
  module_title text NOT NULL,
  module_abbrev text NOT NULL,
  mandatory boolean NOT NULL,
  po jsonb NOT NULL,
  assessment_methods text[] NOT NULL,
  first_examiner text NOT NULL REFERENCES core.identity(id),
  second_examiner text NOT NULL REFERENCES core.identity(id),
  exam_phases text[] NOT NULL,
  exams jsonb NOT NULL,
  UNIQUE (exam_list, module)
);

-- shared exam list configuration of a po
CREATE TABLE exams.exam_list_config(
  po text PRIMARY KEY REFERENCES core.po(id),
  config jsonb NOT NULL
);

-- optimal and maximal seats under exam conditions, seating layout, number of PCs and planning relevant equipment of a
-- room, NULL if unknown
ALTER TABLE schedule.room
  ADD COLUMN exam_capacity_opt integer CHECK (exam_capacity_opt >= 0),
  ADD COLUMN exam_capacity_max integer CHECK (exam_capacity_max >= 0),
  ADD CHECK (exam_capacity_opt <= exam_capacity_max),
  ADD COLUMN seating text CHECK (seating IN ('tiered', 'fixed', 'loose', 'none')),
  ADD COLUMN pcs integer CHECK (pcs >= 0),
  ADD COLUMN equipment text[] NOT NULL DEFAULT '{}' CHECK (equipment <@ ARRAY['projector', 'display', 'document_camera', 'chalkboard', 'whiteboard', 'sound_system', 'video_conference', 'moderation_kit']);

-- shared selection of the planning pool of an exam plan draft, see models.schedule.ExamPoolConfig
CREATE TABLE schedule.exam_pool_config(
  plan_draft uuid PRIMARY KEY REFERENCES schedule.plan_draft(id) ON DELETE CASCADE,
  config jsonb NOT NULL
);

-- exam appointment of an exam plan draft, module and examiner data are resolved from the exam's modules
-- assessment_method: selected by the appointment, NULL if unspecified
CREATE TABLE schedule.exam_entry_draft(
  id uuid PRIMARY KEY,
  plan_draft uuid NOT NULL REFERENCES schedule.plan_draft(id) ON DELETE CASCADE,
  exam uuid NOT NULL REFERENCES exams.exam(id),
  phase text NOT NULL,
  "start" timestamptz NOT NULL,
  "end" timestamptz NOT NULL,
  expected_participants integer CHECK (expected_participants >= 0),
  assessment_method text REFERENCES core.assessment_method(id),
  CHECK ("end" > "start")
);

CREATE INDEX idx_exam_entry_draft_plan_draft ON schedule.exam_entry_draft(plan_draft);

-- room of an exam appointment, unknown counts are NULL, never fewer required than assigned invigilators
-- partition_by, partition_value: criterion and value partitioning the participants into the rooms, e.g. Nachname and
-- A-Me, both trimmed and nonblank or both NULL if unspecified
CREATE TABLE schedule.exam_room_draft(
  exam_entry_draft uuid NOT NULL REFERENCES schedule.exam_entry_draft(id) ON DELETE CASCADE,
  room uuid NOT NULL REFERENCES schedule.room(id),
  allocated_seats integer CHECK (allocated_seats >= 0),
  required_invigilators integer CHECK (required_invigilators >= 0),
  invigilators text[] NOT NULL,
  partition_by text CHECK (partition_by = btrim(partition_by) AND partition_by <> ''),
  partition_value text CHECK (partition_value = btrim(partition_value) AND partition_value <> ''),
  PRIMARY KEY (exam_entry_draft, room),
  CHECK (cardinality(invigilators) <= required_invigilators),
  CHECK ((partition_by IS NULL) =(partition_value IS NULL))
);

-- published exam appointment, kept in sync with its source draft appointment by publication
-- planning figures stay in the drafts and are not published
CREATE TABLE schedule.exam_entry(
  id uuid PRIMARY KEY,
  semester text NOT NULL CHECK (semester ~ '^(wise|sose)_[0-9]{4}$'),
  exam uuid NOT NULL REFERENCES exams.exam(id),
  phase text NOT NULL,
  "start" timestamptz NOT NULL,
  "end" timestamptz NOT NULL,
  assessment_method text REFERENCES core.assessment_method(id),
  -- the source drafts have no foreign keys, so deleting drafts changes published appointments only on publication
  source_plan_draft uuid,
  source_exam_entry_draft uuid,
  CHECK ("end" > "start")
);

CREATE INDEX idx_exam_entry_source_plan_draft ON schedule.exam_entry(source_plan_draft);

CREATE UNIQUE INDEX idx_exam_entry_source_exam_entry_draft ON schedule.exam_entry(source_exam_entry_draft)
WHERE
  source_exam_entry_draft IS NOT NULL;

CREATE TABLE schedule.exam_room(
  exam_entry uuid NOT NULL REFERENCES schedule.exam_entry(id) ON DELETE CASCADE,
  room uuid NOT NULL REFERENCES schedule.room(id),
  invigilators text[] NOT NULL,
  partition_by text CHECK (partition_by = btrim(partition_by) AND partition_by <> ''),
  partition_value text CHECK (partition_value = btrim(partition_value) AND partition_value <> ''),
  PRIMARY KEY (exam_entry, room),
  CHECK ((partition_by IS NULL) =(partition_value IS NULL))
);

-- !Downs
DROP TABLE schedule.exam_room;

DROP TABLE schedule.exam_entry;

DROP TABLE schedule.exam_room_draft;

DROP TABLE schedule.exam_entry_draft;

DROP TABLE schedule.exam_pool_config;

ALTER TABLE schedule.room
  DROP COLUMN equipment,
  DROP COLUMN pcs,
  DROP COLUMN seating,
  DROP COLUMN exam_capacity_max,
  DROP COLUMN exam_capacity_opt;

DROP TABLE exams.exam_list_config;

DROP TABLE exams.exam_list_entry;

-- keeps the latest semester of each po to restore the primary key on po
DELETE FROM exams.exam_list l USING exams.exam_list o
WHERE o.po = l.po
  AND split_part(o.semester, '_', 2) || split_part(o.semester, '_', 1) > split_part(l.semester, '_', 2) || split_part(l.semester, '_', 1);

ALTER TABLE exams.exam_list
  DROP CONSTRAINT exam_list_po_semester_key;

ALTER TABLE exams.exam_list
  DROP COLUMN id;

ALTER TABLE exams.exam_list
  ADD PRIMARY KEY (po);

ALTER TABLE exams.exam_list SET SCHEMA modules;

DROP TABLE exams.exam_module;

DROP TABLE exams.exam;

