-- !Ups
ALTER TABLE modules.module_po_mandatory
  ADD COLUMN recommended_semester_part_time integer NULL;

-- !Downs
ALTER TABLE modules.module_po_mandatory
  DROP COLUMN recommended_semester_part_time;
