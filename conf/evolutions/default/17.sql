-- !Ups
ALTER TABLE modules.exam_list
  DROP CONSTRAINT IF EXISTS exam_list_pkey;

ALTER TABLE modules.exam_list
  ADD PRIMARY KEY (po);

CREATE TABLE modules.module_catalog(
  po text PRIMARY KEY REFERENCES core.po(id),
  semester text NOT NULL,
  date date NOT NULL,
  url text NOT NULL
);

-- !Downs
ALTER TABLE modules.exam_list
  DROP CONSTRAINT IF EXISTS exam_list_pkey;

ALTER TABLE modules.exam_list
  ADD PRIMARY KEY (po, semester);

DROP TABLE modules.module_catalog;

