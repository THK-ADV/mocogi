-- adds support for RPOs

-- !Ups

alter table assessment_method
    add column "source" text not null default 'unknown';

create table permitted_assessment_method_for_module
(
    "module"             uuid primary key not null,
    "assessment_methods" text[]           not null
);

update assessment_method
set source = 'rpo'
where id in (
             'written-exam',
             'written-exam-answer-choice-method',
             'oral-exam',
             'home-assignment',
             'open-book-exam',
             'project',
             'portfolio',
             'practical-report',
             'oral-contribution',
             'certificate-achievement',
             'performance-assessment',
             'role-play',
             'admission-colloquium',
             'specimen'
    );
