-- !Ups
CREATE TABLE modules.module_catalog(
  po text PRIMARY KEY REFERENCES core.po(id),
  semester text NOT NULL,
  date date NOT NULL,
  url text NOT NULL
);

-- !Downs
DROP TABLE modules.module_catalog;
