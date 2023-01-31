-- !Ups

alter table user_has_branch
    add column commit_id text null;

-- !Downs
alter table user_has_branch
    drop column commit_id;