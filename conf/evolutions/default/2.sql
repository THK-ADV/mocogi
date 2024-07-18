-- Update study_program

-- !Ups

alter table study_program
    drop column external_abbreviation,
    drop column internal_abbreviation;

-- !Downs

alter table study_program
    add column external_abbreviation text not null default '',
    add column internal_abbreviation text not null default '';