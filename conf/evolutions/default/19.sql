-- !Ups
CREATE SCHEMA IF NOT EXISTS exams;

-- exams of a module with own exams (neither generic nor child module)
-- name = NULL: the module has a single default exam named after the module itself
-- name <> NULL: an explicitly named exam, the default row is replaced once names are set
CREATE TABLE exams.module_exam (
  module uuid NOT NULL REFERENCES modules.module(id) ON DELETE CASCADE,
  name text,
  UNIQUE NULLS NOT DISTINCT (module, name)
);

-- bootstrapping: gives every existing module with own exams its default exam (name = NULL)
INSERT INTO exams.module_exam (module)
SELECT m.id
FROM modules.module m
WHERE m.module_type <> 'generic_module'
  AND NOT EXISTS (SELECT 1 FROM modules.module_relation r WHERE r.child = m.id);

-- !Downs
DROP TABLE exams.module_exam;
