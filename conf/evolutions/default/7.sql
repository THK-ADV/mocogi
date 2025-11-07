-- !Ups

create table permission
(
    "id"      serial primary key,
    "type"    text   not null,
    "person"  text   not null,
    "context" text[] null,
    foreign key (person) references identity (id)
);
