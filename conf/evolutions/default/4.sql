-- adds examiner and exam phases

-- !Ups

alter table module add column "first_examiner" text not null default 'nn';
alter table module add constraint "module_first_examiner_fkey" FOREIGN KEY (first_examiner) REFERENCES identity(id);
alter table module add column "second_examiner" text not null default 'nn';
alter table module add constraint "module_second_examiner_fkey" FOREIGN KEY (second_examiner) REFERENCES identity(id);
alter table module add column "exam_phases" text not null default 'none';