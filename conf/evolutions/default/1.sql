-- !Ups

create table location
(
    "id"       text primary key,
    "de_label" text not null,
    "en_label" text not null
);

create table language
(
    "id"       text primary key,
    "de_label" text not null,
    "en_label" text not null
);

create table status
(
    "id"       text primary key,
    "de_label" text not null,
    "en_label" text not null
);

create table assessment_method
(
    "id"       text primary key,
    "de_label" text not null,
    "en_label" text not null,
    "source"   text not null
);

create table module_type
(
    "id"       text primary key,
    "de_label" text not null,
    "en_label" text not null
);

create table season
(
    "id"       text primary key,
    "de_label" text not null,
    "en_label" text not null
);

create table degree
(
    "id"       text primary key,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table identity
(
    "id"              text primary key,
    "title"           text    not null,
    "is_active"       boolean not null,
    "kind"            text    not null,
    "employment_type" text    null,
    "image_url"       text    null,
    "website_url"     text    null,
    "lastname"        text    null,
    "firstname"       text    null,
    "abbreviation"    text    null,
    "campus_id"       text    null,
    "faculties"       text[]  null
);

create table study_program
(
    "id"       text primary key,
    "de_label" text not null,
    "en_label" text not null,
    "degree"   text not null,
    foreign key (degree) references degree (id)
);

create table study_program_person
(
    "person"        text not null,
    "study_program" text not null,
    "role"          text not null,
    primary key (person, study_program, role),
    foreign key (person) references identity (id),
    foreign key (study_program) references study_program (id)
);

create table po
(
    "id"            text primary key,
    "study_program" text     not null,
    "version"       smallint not null,
    "date_from"     date     not null,
    "date_to"       date,
    foreign key (study_program) references study_program (id)
);

create table specialization
(
    "id"    text primary key,
    "po"    text not null,
    "label" text not null,
    foreign key (po) references po (id)
);

create table module
(
    "id"                        uuid primary key,
    "last_modified"             timestamp     not null,
    "title"                     text          not null,
    "abbrev"                    text          not null,
    "module_type"               text          not null,
    "ects"                      numeric(4, 2) not null,
    "language"                  text          not null,
    "duration"                  smallint      not null,
    "season"                    text          not null,
    "workload"                  jsonb         not null,
    "status"                    text          not null,
    "location"                  text          not null,
    "first_examiner"            text          not null,
    "second_examiner"           text          not null,
    "exam_phases"               text[]        not null,
    "participants"              jsonb         null,
    "recommended_prerequisites" jsonb         null,
    "required_prerequisites"    jsonb         null,
    "learning_outcome_de"       text          not null,
    "learning_outcome_en"       text          not null,
    "module_content_de"         text          not null,
    "module_content_en"         text          not null,
    "learning_methods_de"       text          not null,
    "learning_methods_en"       text          not null,
    "literature_de"             text          not null,
    "literature_en"             text          not null,
    "particularities_de"        text          not null,
    "particularities_en"        text          not null,
    foreign key (module_type) references module_type (id),
    foreign key (language) references language (id),
    foreign key (season) references season (id),
    foreign key (status) references status (id),
    foreign key (location) references location (id),
    foreign key (first_examiner) references identity (id),
    foreign key (second_examiner) references identity (id)
);

create table permitted_assessment_method_for_module
(
    "module"             uuid primary key not null,
    "assessment_methods" text[]           not null
);

create table module_relation
(
    "module"          uuid not null,
    "relation_type"   text not null,
    "relation_module" uuid not null,
    primary key (module, relation_type, relation_module),
    foreign key (module) references module (id),
    foreign key (relation_module) references module (id)
);

create table module_responsibility
(
    "module"              uuid not null,
    "identity"            text not null,
    "responsibility_type" text not null,
    primary key (module, identity, responsibility_type),
    foreign key (module) references module (id),
    foreign key (identity) references identity (id)
);

create table module_assessment_method
(
    "id"                uuid          not null primary key,
    "module"            uuid          not null,
    "assessment_method" text          not null,
    "percentage"        numeric(5, 2) null,
    "precondition"      text[]        null,
    foreign key (assessment_method) references assessment_method (id),
    foreign key (module) references module (id)
);

create table module_po_mandatory
(
    "id"                   uuid      not null primary key,
    "module"               uuid      not null,
    "po"                   text      not null,
    "recommended_semester" integer[] not null,
    "specialization"       text      null,
    foreign key (specialization) references specialization (id),
    foreign key (module) references module (id),
    foreign key (po) references po (id)
);

create table module_po_optional
(
    "id"                   uuid      not null primary key,
    "module"               uuid      not null,
    "po"                   text      not null,
    "instance_of"          uuid      not null,
    "part_of_catalog"      boolean   not null,
    "recommended_semester" integer[] not null,
    "specialization"       text      null,
    foreign key (specialization) references specialization (id),
    foreign key (module) references module (id),
    foreign key (po) references po (id),
    foreign key (instance_of) references module (id)
);

create table module_taught_with
(
    "module"        uuid not null,
    "module_taught" uuid not null,
    primary key (module, module_taught),
    foreign key (module_taught) references module (id),
    foreign key (module) references module (id)
);

-- git handling

-- representation of a module currently in editing state
create table module_draft
(
    "module"                uuid      not null primary key,
    "module_title"          text      not null,
    "module_abbrev"         text      not null,
    "author"                text      not null,
    "branch"                text      not null,
    "source"                text      not null,
    "module_json"           jsonb     not null,
    "module_json_validated" jsonb     not null,
    "module_print"          text      not null,
    "keys_to_be_reviewed"   text      not null,
    "modified_keys"         text      not null,
    "last_commit_id"        text      null,
    "merge_request_id"      integer   null,
    "merge_request_status"  text      null,
    "last_modified"         timestamp not null,
    foreign key (author) references identity (id)
);

-- tracks fresh created modules which are only in the draft branch and therefore not available to the app
create table created_module_in_draft
(
    "module"               uuid          not null primary key,
    "module_title"         text          not null,
    "module_abbrev"        text          not null,
    "module_management"    text[]        not null,
    "module_ects"          numeric(4, 2) not null,
    "module_type"          text          not null,
    "module_mandatory_pos" text[]        not null,
    "module_optional_pos"  text[]        not null
);

-- tracks the relation between a module and their po companion file
create table module_companion
(
    "module"       uuid not null,
    "companion_po" text not null,
    primary key (module, companion_po),
    foreign key (companion_po) references po (id)
);

-- tracks who are eligible to edit modules
create table module_update_permission
(
    "module"    uuid not null,
    "campus_id" text not null,
    "kind"      text not null,
    primary key (module, campus_id)
);

-- module reviews by role
create table module_review
(
    "id"            uuid      not null primary key,
    "module_draft"  uuid      not null,
    "role"          text      not null,
    "status"        text      not null,
    "study_program" text      not null,
    "comment"       text      null,
    "responded_by"  text      null,
    "responded_at"  timestamp null,
    foreign key (study_program) references study_program (id),
    foreign key (module_draft) references module_draft (module),
    foreign key (responded_by) references identity (id)
);

-- module catalog

create table module_catalog
(
    "full_po"        text      not null primary key,
    "po"             text      not null,
    "specialization" text      null,
    "study_program"  text      not null,
    "semester"       text      not null,
    "de_url"         text      not null,
    "en_url"         text      not null,
    "generated"      timestamp not null,
    foreign key (study_program) references study_program (id),
    foreign key (po) references po (id)
);

create table module_catalog_generation_request
(
    "merge_request_id"     integer not null,
    "merge_request_status" text    not null,
    "semester"             text    not null,
    primary key (merge_request_id, semester)
);

-- !Downs
drop table module_catalog_generation_request if exists;
drop table module_catalog if exists;
drop table module_review if exists;
drop table module_update_permission if exists;
drop table module_companion if exists;
drop table created_module_in_draft if exists;
drop table module_draft if exists;
drop table module_taught_with if exists;
drop table module_po_optional if exists;
drop table module_po_mandatory if exists;
drop table module_assessment_method if exists;
drop table module_responsibility if exists;
drop table module_relation if exists;
drop table permitted_assessment_method_for_module if exists;
drop table module if exists;
drop table specialization if exists;
drop table po if exists;
drop table study_program_person if exists;
drop table study_program if exists;
drop table identity if exists;
drop table global_criteria if exists;
drop table degree if exists;
drop table season if exists;
drop table module_type if exists;
drop table assessment_method if exists;
drop table status if exists;
drop table language if exists;
drop table location if exists;
