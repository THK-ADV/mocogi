-- !Ups

create table exam_list
(
    "po"       text not null,
    "semester" text not null,
    "date"     date not null,
    "url"      text not null,
    primary key (po, semester),
    foreign key (po) references po (id)
);
