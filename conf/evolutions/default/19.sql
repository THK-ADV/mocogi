-- !Ups
CREATE SCHEMA IF NOT EXISTS exams;

-- exams of a module with own exams (neither generic nor child module)
-- name = NULL: the module has a single default exam named after the module itself
-- name <> NULL: an explicitly named exam, the default row is replaced once names are set
CREATE TABLE exams.module_exam(
  module uuid NOT NULL REFERENCES modules.module(id) ON DELETE CASCADE,
  name text,
  UNIQUE NULLS NOT DISTINCT (module, name)
);

-- bootstrapping: gives every existing module with own exams its default exam (name = NULL)
INSERT INTO exams.module_exam(module)
SELECT
  m.id
FROM
  modules.module m
WHERE
  m.module_type <> 'generic_module'
  AND NOT EXISTS (
    SELECT
      1
    FROM
      modules.module_relation r
    WHERE
      r.child = m.id);

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
-- module has no foreign key, so published entries outlive the module
-- exams: [{ name, registrationPeriods: { phase: { start, end } } }] where name = NULL refers to module_title
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

-- !Downs
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

DROP TABLE exams.module_exam;

