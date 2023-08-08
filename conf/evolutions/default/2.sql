-- !Ups

alter table person
    add column "campus_id" text null;

-- !Downs

alter table person
    drop column "campus_id";