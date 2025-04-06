-- !Ups

alter table study_program
    add column abbreviation text not null default '';
alter table specialization
    add column abbreviation text not null default '';
