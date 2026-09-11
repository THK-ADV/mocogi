-- !Ups
CREATE UNIQUE INDEX permission_person_type_key ON modules.permission ("person", "type");

-- !Downs
DROP INDEX modules.permission_person_type_key;
