-- !Ups

-- tracks the relation between a module and their po companion file
create table module_companion
(
    "module"       uuid not null,
    "companion_po" text not null,
    primary key (module, companion_po),
    foreign key (companion_po) references po (id)
);

-- tracks fresh created modules which are only in the draft branch and therefore not available to the app
create table created_module_in_draft
(
    "module"            uuid          not null primary key,
    "module_title"      text          not null,
    "module_abbrev"     text          not null,
    "module_management" varchar(20)[] not null,
    "module_ects"       numeric(4, 2) not null
);

-- !Downs

drop table module_companion;
drop table created_module_in_draft;
