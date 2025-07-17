-- !Ups

alter table module
    add column attendance_requirement  jsonb default null,
    add column assessment_prerequisite jsonb default null;
