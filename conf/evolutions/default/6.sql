-- !Ups

alter table user_has_branch
    add column commit_id        text    null,
    add column merge_request_id integer null;

-- !Downs
alter table user_has_branch
    drop column commit_id,
    drop column merge_request_id;