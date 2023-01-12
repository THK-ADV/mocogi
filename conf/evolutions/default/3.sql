-- !Ups

create table module_draft
(
    "module_id"     uuid      not null,
    "module_json"   text      not null,
    "branch"        text      not null,
    "status"        text      not null,
    "last_modified" timestamp not null,
    PRIMARY KEY (module_id, branch)
);

-- !Downs
drop table module_draft if exists;
