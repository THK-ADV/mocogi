-- !Ups

create table location
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table language
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table status
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table assessment_method
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table module_type
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table season
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table person
(
    "abbrev"    text PRIMARY KEY,
    "lastname"  text not null,
    "firstname" text not null,
    "title"     text not null,
    "faculty"   text not null
);

create table metadata
(
    "id"                        uuid PRIMARY KEY,
    "git_path"                  text          not null,
    "title"                     text          not null,
    "abbrev"                    text          not null,
    "module_type"               text          not null,
    "children"                  text null,
    "parent"                    text null,
    "credits"                   numeric(4, 2) not null,
    "language"                  text          not null,
    "duration"                  smallint      not null,
    "recommended_semester"      smallint      not null,
    "season"                    text          not null,
    "workload_total"            smallint      not null,
    "workload_lecture"          smallint      not null,
    "workload_seminar"          smallint      not null,
    "workload_practical"        smallint      not null,
    "workload_exercise"         smallint      not null,
    "workload_self_study"       smallint      not null,
    "recommended-prerequisites" text          not null,
    "required-prerequisites"    text          not null,
    "status"                    text          not null,
    "location"                  text          not null,
    "po"                        text          not null,
    FOREIGN KEY (module_type) REFERENCES module_type (abbrev),
    FOREIGN KEY (language) REFERENCES language (abbrev),
    FOREIGN KEY (season) REFERENCES season (abbrev),
    FOREIGN KEY (status) REFERENCES status (abbrev),
    FOREIGN KEY (location) REFERENCES location (abbrev)
);

create table responsibility
(
    "metadata" uuid not null,
    "person"   text not null,
    "kind"     text not null,
    PRIMARY KEY (metadata, person, kind),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (person) REFERENCES person (abbrev)
);

create table assessment_method_metadata
(
    "metadata"          uuid not null,
    "assessment_method" text not null,
    "percentage"        numeric(5, 2) null,
    PRIMARY KEY (metadata, assessment_method, percentage),
    FOREIGN KEY (assessment_method) REFERENCES assessment_method (abbrev),
    FOREIGN KEY (metadata) REFERENCES metadata (id)
);

-- !Downs
drop table assessment_method_metadata if exists;
drop table responsibility if exists;
drop table metadata if exists;
drop table person if exists;
drop table season if exists;
drop table module_type if exists;
drop table assessment_method if exists;
drop table status if exists;
drop table language if exists;
drop table location if exists;