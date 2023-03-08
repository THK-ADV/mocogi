-- !Ups

alter table ects_focus_area_contribution rename description to de_desc;
alter table ects_focus_area_contribution add column en_desc text not null default '';

-- !Downs
alter table ects_focus_area_contribution drop column en_desc;
alter table ects_focus_area_contribution rename de_desc to description;