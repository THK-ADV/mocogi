-- !Ups

create table users
(
    "id"       uuid PRIMARY KEY,
    "username" text not null
);

create table user_has_branch
(
    "user"      uuid PRIMARY KEY,
    "branch_id" text not null,
    FOREIGN KEY ("user") REFERENCES users (id)
);

-- !Downs
drop table users if exists;
drop table user_has_branch if exists;
