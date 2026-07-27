-- !Ups
ALTER TABLE modules.module_relation
  ADD COLUMN "parent" uuid,
  ADD COLUMN "child" uuid;

UPDATE
  modules.module_relation
SET
  "parent" = "module",
  "child" = relation_module
WHERE
  relation_type = 'parent';

-- Legacy child rows were redundant back-references and are not authoritative.
-- A child-only declaration therefore becomes a standalone module.
DELETE FROM modules.module_relation
WHERE relation_type = 'child';

ALTER TABLE modules.module_relation
  ALTER COLUMN "parent" SET NOT NULL,
  ALTER COLUMN "child" SET NOT NULL,
  DROP COLUMN "module",
  DROP COLUMN relation_type,
  DROP COLUMN relation_module;

ALTER TABLE modules.module_relation
  ADD CONSTRAINT module_relation_pkey PRIMARY KEY ("parent", "child"),
  ADD CONSTRAINT module_relation_child_key UNIQUE ("child"),
  ADD CONSTRAINT module_relation_distinct_modules_check CHECK ("parent" <> "child"),
  ADD CONSTRAINT module_relation_parent_fkey FOREIGN KEY ("parent") REFERENCES modules.module(id),
  ADD CONSTRAINT module_relation_child_fkey FOREIGN KEY ("child") REFERENCES modules.module(id);

-- Deployment step: reapply conf/sql/functions.sql after this evolution so
-- resolve_module_relation uses the new parent/child columns.
-- !Downs
-- Rollback step: reapply functions.sql from the previous application version.
ALTER TABLE modules.module_relation
  ADD COLUMN "module" uuid,
  ADD COLUMN relation_type text,
  ADD COLUMN relation_module uuid;

UPDATE
  modules.module_relation
SET
  "module" = "parent",
  relation_type = 'parent',
  relation_module = "child";

ALTER TABLE modules.module_relation
  DROP CONSTRAINT module_relation_pkey,
  DROP CONSTRAINT module_relation_child_key,
  DROP CONSTRAINT module_relation_distinct_modules_check,
  DROP CONSTRAINT module_relation_parent_fkey,
  DROP CONSTRAINT module_relation_child_fkey,
  ALTER COLUMN "parent" DROP NOT NULL,
  ALTER COLUMN "child" DROP NOT NULL;

INSERT INTO modules.module_relation("parent", "child", "module", relation_type, relation_module)
SELECT
  NULL,
  NULL,
  "child",
  'child',
  "parent"
FROM
  modules.module_relation
WHERE
  relation_type = 'parent';

ALTER TABLE modules.module_relation
  ALTER COLUMN "module" SET NOT NULL,
  ALTER COLUMN relation_type SET NOT NULL,
  ALTER COLUMN relation_module SET NOT NULL,
  DROP COLUMN "parent",
  DROP COLUMN "child";

ALTER TABLE modules.module_relation
  ADD CONSTRAINT module_relation_pkey PRIMARY KEY ("module", relation_type, relation_module),
  ADD CONSTRAINT module_relation_module_fkey FOREIGN KEY ("module") REFERENCES modules.module(id),
  ADD CONSTRAINT module_relation_relation_module_fkey FOREIGN KEY (relation_module) REFERENCES modules.module(id);

