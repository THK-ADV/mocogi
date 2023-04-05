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

create table competence
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table faculty
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table grade
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table study_form_type
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table global_criteria
(
    "abbrev"   text PRIMARY KEY,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table person
(
    "id"           text PRIMARY KEY,
    "lastname"     text not null,
    "firstname"    text not null,
    "title"        text not null,
    "abbreviation" text not null,
    "status"       text not null,
    "kind"         text not null
);

create table person_in_faculty
(
    "person"  text not null,
    "faculty" text not null,
    PRIMARY KEY (person, faculty),
    FOREIGN KEY (person) REFERENCES person (id),
    FOREIGN KEY (faculty) REFERENCES faculty (abbrev)
);

create table study_program
(
    "abbrev"                         text PRIMARY KEY,
    "de_label"                       text    not null,
    "en_label"                       text    not null,
    "internal_abbreviation"          text    not null,
    "external_abbreviation"          text    not null,
    "de_url"                         text    not null,
    "en_url"                         text    not null,
    "grade"                          text    not null,
    "program_director"               text    not null,
    "accreditation_until"            date    not null,
    "restricted_admission_value"     boolean not null,
    "restricted_admission_de_reason" text    not null,
    "restricted_admission_en_reason" text    not null,
    "de_description"                 text    not null,
    "de_note"                        text    not null,
    "en_description"                 text    not null,
    "en_note"                        text    not null,
    FOREIGN KEY (grade) REFERENCES grade (abbrev),
    FOREIGN KEY (program_director) REFERENCES person (id)
);

create table study_form
(
    "id"                uuid PRIMARY KEY,
    "study_program"     text     not null,
    "study_form_type"   text     not null,
    "workload_per_ects" smallint not null,
    FOREIGN KEY (study_program) REFERENCES study_program (abbrev),
    FOREIGN KEY (study_form_type) REFERENCES study_form_type (abbrev)
);

create table study_form_scope
(
    "id"         uuid PRIMARY KEY,
    "study_form" uuid     not null,
    "duration"   smallint not null,
    "total_ects" smallint not null,
    "de_reason"  text     not null,
    "en_reason"  text     not null,
    FOREIGN KEY (study_form) REFERENCES study_form (id)
);

create table study_program_language
(
    "language"      text not null,
    "study_program" text not null,
    PRIMARY KEY (language, study_program),
    FOREIGN KEY (language) REFERENCES language (abbrev),
    FOREIGN KEY (study_program) REFERENCES study_program (abbrev)
);

create table study_program_season
(
    "season"        text not null,
    "study_program" text not null,
    PRIMARY KEY (season, study_program),
    FOREIGN KEY (season) REFERENCES season (abbrev),
    FOREIGN KEY (study_program) REFERENCES study_program (abbrev)
);

create table study_program_location
(
    "location"      text not null,
    "study_program" text not null,
    PRIMARY KEY (location, study_program),
    FOREIGN KEY (location) REFERENCES location (abbrev),
    FOREIGN KEY (study_program) REFERENCES study_program (abbrev)
);

create table focus_area
(
    "abbrev"        text PRIMARY KEY,
    "study_program" text not null,
    "de_label"      text not null,
    "de_desc"       text not null,
    "en_label"      text not null,
    "en_desc"       text not null,
    FOREIGN KEY (study_program) REFERENCES study_program (abbrev)
);

create table po
(
    "abbrev"        text PRIMARY KEY,
    "study_program" text     not null,
    "version"       smallint not null,
    "date"          date     not null,
    "date_from"     date     not null,
    "date_to"       date,
    FOREIGN KEY (study_program) REFERENCES study_program (abbrev)
);

create table po_modification_date
(
    "po"   text not null,
    "date" date not null,
    PRIMARY KEY (po, date),
    FOREIGN KEY (po) REFERENCES po (abbrev)
);

create table specialization
(
    "abbrev" text PRIMARY KEY,
    "po"     text not null,
    "label"  text not null,
    FOREIGN KEY (po) REFERENCES po (abbrev)
);

create table metadata
(
    "id"                           uuid PRIMARY KEY,
    "git_path"                     text          not null,
    "last_modified"                timestamp     not null,
    "title"                        text          not null,
    "abbrev"                       text          not null,
    "module_type"                  text          not null,
    "ects"                         numeric(4, 2) not null,
    "language"                     text          not null,
    "duration"                     smallint      not null,
    "season"                       text          not null,
    "workload_lecture"             smallint      not null,
    "workload_seminar"             smallint      not null,
    "workload_practical"           smallint      not null,
    "workload_exercise"            smallint      not null,
    "workload_project_supervision" smallint      not null,
    "workload_project_work"        smallint      not null,
    "workload_self_study"          smallint      not null,
    "workload_total"               smallint      not null,
    "status"                       text          not null,
    "location"                     text          not null,
    "participants_min"             smallint null,
    "participants_max"             smallint null,
    "learning_outcome_de"          text          not null,
    "learning_outcome_en"          text          not null,
    "module_content_de"            text          not null,
    "module_content_en"            text          not null,
    "learning_methods_de"          text          not null,
    "learning_methods_en"          text          not null,
    "literature_de"                text          not null,
    "literature_en"                text          not null,
    "particularities_de"           text          not null,
    "particularities_en"           text          not null,
    FOREIGN KEY (module_type) REFERENCES module_type (abbrev),
    FOREIGN KEY (language) REFERENCES language (abbrev),
    FOREIGN KEY (season) REFERENCES season (abbrev),
    FOREIGN KEY (status) REFERENCES status (abbrev),
    FOREIGN KEY (location) REFERENCES location (abbrev)
);

create table ects_focus_area_contribution
(
    "focus_area" text          not null,
    "metadata"   uuid          not null,
    "ects_value" numeric(4, 2) not null,
    "de_desc"    text          not null,
    "en_desc"    text          not null,
    PRIMARY KEY (focus_area, metadata),
    FOREIGN KEY (focus_area) REFERENCES focus_area (abbrev),
    FOREIGN KEY (metadata) REFERENCES metadata (id)
);

create table module_relation
(
    "module"          uuid not null,
    "relation_type"   text not null,
    "relation_module" uuid not null,
    PRIMARY KEY (module, relation_type, relation_module),
    FOREIGN KEY (module) REFERENCES metadata (id),
    FOREIGN KEY (relation_module) REFERENCES metadata (id)
);

create table responsibility
(
    "metadata"            uuid not null,
    "person"              text not null,
    "responsibility_type" text not null,
    PRIMARY KEY (metadata, person, responsibility_type),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (person) REFERENCES person (id)
);

create table metadata_assessment_method
(
    "id"                     uuid not null PRIMARY KEY,
    "metadata"               uuid not null,
    "assessment_method"      text not null,
    "assessment_method_type" text not null,
    "percentage"             numeric(5, 2) null,
    FOREIGN KEY (assessment_method) REFERENCES assessment_method (abbrev),
    FOREIGN KEY (metadata) REFERENCES metadata (id)
);

create table metadata_assessment_method_precondition
(
    "assessment_method"          text not null,
    "metadata_assessment_method" uuid not null,
    PRIMARY KEY (assessment_method, metadata_assessment_method),
    FOREIGN KEY (assessment_method) REFERENCES assessment_method (abbrev),
    FOREIGN KEY (metadata_assessment_method) REFERENCES metadata_assessment_method (id)
);

create table prerequisites
(
    "id"                uuid PRIMARY KEY,
    "metadata"          uuid not null,
    "prerequisite_type" text not null,
    "text"              text not null,
    FOREIGN KEY (metadata) REFERENCES metadata (id)
);

create table prerequisites_module
(
    "prerequisites" uuid not null,
    "module"        uuid not null,
    PRIMARY KEY (prerequisites, module),
    FOREIGN KEY (prerequisites) REFERENCES prerequisites (id),
    FOREIGN KEY (module) REFERENCES metadata (id)
);

create table prerequisites_po
(
    "prerequisites" uuid not null,
    "po"            text not null,
    PRIMARY KEY (prerequisites, po),
    FOREIGN KEY (prerequisites) REFERENCES prerequisites (id),
    FOREIGN KEY (po) REFERENCES po (abbrev)
);

create table po_mandatory
(
    "id"                             uuid not null PRIMARY KEY,
    "metadata"                       uuid not null,
    "po"                             text not null,
    "recommended_semester"           text not null,
    "recommended_semester_part_time" text not null,
    "specialization"                 text null,
    FOREIGN KEY (specialization) REFERENCES specialization (abbrev),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (po) REFERENCES po (abbrev)
);

create table po_optional
(
    "id"                   uuid    not null PRIMARY KEY,
    "metadata"             uuid    not null,
    "po"                   text    not null,
    "instance_of"          uuid    not null,
    "part_of_catalog"      boolean not null,
    "recommended_semester" text    not null,
    "specialization"       text null,
    FOREIGN KEY (specialization) REFERENCES specialization (abbrev),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (po) REFERENCES po (abbrev),
    FOREIGN KEY (instance_of) REFERENCES metadata (id)
);

create table metadata_competence
(
    "metadata"   uuid not null,
    "competence" text not null,
    PRIMARY KEY (metadata, competence),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (competence) REFERENCES competence (abbrev)
);

create table metadata_global_criteria
(
    "metadata"        uuid not null,
    "global_criteria" text not null,
    PRIMARY KEY (metadata, global_criteria),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (global_criteria) REFERENCES global_criteria (abbrev)
);

create table metadata_taught_with
(
    "metadata" uuid not null,
    "module"   uuid not null,
    PRIMARY KEY (metadata, module),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (module) REFERENCES metadata (id)
);

-- git handling

create table users
(
    "id"       uuid PRIMARY KEY,
    "username" text not null
);

create table user_has_branch
(
    "user"             uuid PRIMARY KEY,
    "branch_id"        text not null,
    "commit_id"        text null,
    "merge_request_id" integer null,
    FOREIGN KEY ("user") REFERENCES users (id)
);

create table module_draft
(
    "module_id"                    uuid      not null,
    "module_json"                  text      not null,
    "branch"                       text      not null,
    "status"                       text      not null,
    "last_modified"                timestamp not null,
    "valid_module_compendium_json" text null,
    "module_compendium_print"      text null,
    "pipeline_error"               text null,
    PRIMARY KEY (module_id, branch)
);

-- !Downs
drop table module_draft if exists;
drop table users if exists;
drop table user_has_branch if exists;
drop table metadata_taught_with if exists;
drop table metadata_global_criteria if exists;
drop table metadata_competence if exists;
drop table po_optional if exists;
drop table po_mandatory if exists;
drop table prerequisites_po if exists;
drop table prerequisites_module if exists;
drop table prerequisites if exists;
drop table metadata_assessment_method_precondition if exists;
drop table metadata_assessment_method if exists;
drop table responsibility if exists;
drop table module_relation if exists;
drop table ects_focus_area_contribution if exists;
drop table metadata if exists;
drop table specialization if exists;
drop table po_modification_date if exists;
drop table po if exists;
drop table focus_area if exists;
drop table study_program_location if exists;
drop table study_program_season if exists;
drop table study_program_language if exists;
drop table study_form_scope if exists;
drop table study_form if exists;
drop table study_program if exists;
drop table person_in_faculty if exists;
drop table person if exists;
drop table global_criteria if exists;
drop table study_form_type if exists;
drop table grade if exists;
drop table faculty if exists;
drop table competence if exists;
drop table season if exists;
drop table module_type if exists;
drop table assessment_method if exists;
drop table status if exists;
drop table language if exists;
drop table location if exists;
