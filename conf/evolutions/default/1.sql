-- !Ups

create table location
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table language
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table status
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table assessment_method
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table module_type
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table season
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table competence
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table faculty
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
);

create table degree
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table global_criteria
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table identity
(
    "id"           text PRIMARY KEY,
    "lastname"     text not null,
    "firstname"    text not null,
    "title"        text not null,
    "abbreviation" text not null,
    "campus_id"    text null,
    "status"       text not null,
    "kind"         text not null
);

create table person_in_faculty
(
    "person"  text not null,
    "faculty" text not null,
    PRIMARY KEY (person, faculty),
    FOREIGN KEY (person) REFERENCES identity (id),
    FOREIGN KEY (faculty) REFERENCES faculty (id)
);

create table study_program
(
    "id"                    text PRIMARY KEY,
    "de_label"              text not null,
    "en_label"              text not null,
    "internal_abbreviation" text not null,
    "external_abbreviation" text not null,
    "degree"                text not null,
    FOREIGN KEY (degree) REFERENCES degree (id)
);

create table study_program_person
(
    "person"        text not null,
    "study_program" text not null,
    "role"          text not null,
    PRIMARY KEY (person, study_program, role),
    FOREIGN KEY (person) REFERENCES identity (id),
    FOREIGN KEY (study_program) REFERENCES study_program (id)
);

create table focus_area
(
    "id"            text PRIMARY KEY,
    "study_program" text not null,
    "de_label"      text not null,
    "de_desc"       text not null,
    "en_label"      text not null,
    "en_desc"       text not null,
    FOREIGN KEY (study_program) REFERENCES study_program (id)
);

create table po
(
    "id"            text PRIMARY KEY,
    "study_program" text     not null,
    "version"       smallint not null,
    "date_from"     date     not null,
    "date_to"       date,
    FOREIGN KEY (study_program) REFERENCES study_program (id)
);

create table specialization
(
    "id"    text PRIMARY KEY,
    "po"    text not null,
    "label" text not null,
    FOREIGN KEY (po) REFERENCES po (id)
);

create table module
(
    "id"                           uuid PRIMARY KEY,
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
    FOREIGN KEY (module_type) REFERENCES module_type (id),
    FOREIGN KEY (language) REFERENCES language (id),
    FOREIGN KEY (season) REFERENCES season (id),
    FOREIGN KEY (status) REFERENCES status (id),
    FOREIGN KEY (location) REFERENCES location (id)
);

create table module_ects_focus_area_contribution
(
    "focus_area" text          not null,
    "module"     uuid          not null,
    "ects_value" numeric(4, 2) not null,
    "de_desc"    text          not null,
    "en_desc"    text          not null,
    PRIMARY KEY (focus_area, module),
    FOREIGN KEY (focus_area) REFERENCES focus_area (id),
    FOREIGN KEY (module) REFERENCES module (id)
);

create table module_relation
(
    "module"          uuid not null,
    "relation_type"   text not null,
    "relation_module" uuid not null,
    PRIMARY KEY (module, relation_type, relation_module),
    FOREIGN KEY (module) REFERENCES module (id),
    FOREIGN KEY (relation_module) REFERENCES module (id)
);

create table module_responsibility
(
    "module"              uuid not null,
    "identity"            text not null,
    "responsibility_type" text not null,
    PRIMARY KEY (module, identity, responsibility_type),
    FOREIGN KEY (module) REFERENCES module (id),
    FOREIGN KEY (identity) REFERENCES identity (id)
);

create table module_assessment_method
(
    "id"                     uuid not null PRIMARY KEY,
    "module"                 uuid not null,
    "assessment_method"      text not null,
    "assessment_method_type" text not null,
    "percentage"             numeric(5, 2) null,
    FOREIGN KEY (assessment_method) REFERENCES assessment_method (id),
    FOREIGN KEY (module) REFERENCES module (id)
);

create table module_assessment_method_precondition
(
    "assessment_method"        text not null,
    "module_assessment_method" uuid not null,
    PRIMARY KEY (assessment_method, module_assessment_method),
    FOREIGN KEY (assessment_method) REFERENCES assessment_method (id),
    FOREIGN KEY (module_assessment_method) REFERENCES module_assessment_method (id)
);

create table module_prerequisites
(
    "id"                uuid PRIMARY KEY,
    "module"            uuid not null,
    "prerequisite_type" text not null,
    "text"              text not null,
    FOREIGN KEY (module) REFERENCES module (id)
);

create table prerequisites_module
(
    "prerequisites" uuid not null,
    "module"        uuid not null,
    PRIMARY KEY (prerequisites, module),
    FOREIGN KEY (prerequisites) REFERENCES module_prerequisites (id),
    FOREIGN KEY (module) REFERENCES module (id)
);

create table prerequisites_po
(
    "prerequisites" uuid not null,
    "po"            text not null,
    PRIMARY KEY (prerequisites, po),
    FOREIGN KEY (prerequisites) REFERENCES module_prerequisites (id),
    FOREIGN KEY (po) REFERENCES po (id)
);

create table module_po_mandatory
(
    "id"                   uuid not null PRIMARY KEY,
    "module"               uuid not null,
    "po"                   text not null,
    "recommended_semester" text not null,
    "specialization"       text null,
    FOREIGN KEY (specialization) REFERENCES specialization (id),
    FOREIGN KEY (module) REFERENCES module (id),
    FOREIGN KEY (po) REFERENCES po (id)
);

create table module_po_optional
(
    "id"                   uuid    not null PRIMARY KEY,
    "module"               uuid    not null,
    "po"                   text    not null,
    "instance_of"          uuid null,
    "focus"                boolean not null,
    "recommended_semester" text    not null,
    "specialization"       text null,
    FOREIGN KEY (specialization) REFERENCES specialization (id),
    FOREIGN KEY (module) REFERENCES module (id),
    FOREIGN KEY (po) REFERENCES po (id),
    FOREIGN KEY (instance_of) REFERENCES module (id)
);

create table module_competence
(
    "module"     uuid not null,
    "competence" text not null,
    PRIMARY KEY (module, competence),
    FOREIGN KEY (module) REFERENCES module (id),
    FOREIGN KEY (competence) REFERENCES competence (id)
);

create table module_global_criteria
(
    "module"          uuid not null,
    "global_criteria" text not null,
    PRIMARY KEY (module, global_criteria),
    FOREIGN KEY (module) REFERENCES module (id),
    FOREIGN KEY (global_criteria) REFERENCES global_criteria (id)
);

create table module_taught_with
(
    "module"        uuid not null,
    "module_taught" uuid not null,
    PRIMARY KEY (module, module_taught),
    FOREIGN KEY (module_taught) REFERENCES module (id),
    FOREIGN KEY (module) REFERENCES module (id)
);

-- git handling

create table module_draft
(
    "module"                 uuid      not null PRIMARY KEY,
    "module_title"           text      not null,
    "module_abbrev"          text      not null,
    "author"                 text      not null,
    "branch"                 text      not null,
    "source"                 text      not null,
    "module_json"            text      not null,
    "module_validated_json"  text      not null,
    "module_validated_print" text      not null,
    "keys_to_be_reviewed"    text      not null,
    "modified_keys"          text      not null,
    "last_commit_id"         text null,
    "merge_request_id"       integer null,
    "merge_request_status"   text null,
    "last_modified"          timestamp not null,
    FOREIGN KEY (author) REFERENCES identity (id)
);

create table module_update_permission
(
    "module"    uuid not null,
    "campus_id" text not null,
    "kind"      text not null,
    PRIMARY KEY (module, campus_id)
);

create table module_review
(
    "id"            uuid not null PRIMARY KEY,
    "module_draft"  uuid not null,
    "role"          text not null,
    "status"        text not null,
    "study_program" text not null,
    "comment"       text null,
    "responded_by"  text null,
    "responded_at"  timestamp null,
    FOREIGN KEY (study_program) REFERENCES study_program (id),
    FOREIGN KEY (module_draft) REFERENCES module_draft (module),
    FOREIGN KEY (responded_by) REFERENCES identity (id)
);

-- module catalog

create table module_catalog
(
    "full_po"        text      not null PRIMARY KEY,
    "po"             text      not null,
    "specialization" text null,
    "study_program"  text      not null,
    "semester"       text      not null,
    "de_url"         text      not null,
    "en_url"         text      not null,
    "generated"      timestamp not null,
    FOREIGN KEY (study_program) REFERENCES study_program (id),
    FOREIGN KEY (po) REFERENCES po (id)
);

create table module_catalog_generation_request
(
    "merge_request_id"     integer not null,
    "merge_request_status" text    not null,
    "semester"             text    not null,
    PRIMARY KEY (merge_request_id, semester)
);

-- study_program_view

create
materialized view study_program_view as
select study_program.de_label as sp_de_label,
       study_program.en_label as sp_en_label,
       study_program.id       as sp_id,
       degree.id              as degree_id,
       degree.de_label        as degree_de_label,
       degree.en_label        as degree_en_label,
       degree.de_desc         as degree_de_desc,
       degree.en_desc         as degree_en_desc,
       po.id                  as po_id,
       po.version             as po_version,
       specialization.label   as spec_label,
       specialization.id      as spec_id
from study_program
         join degree on study_program.degree = degree.id
         join po on po.study_program = study_program.id and
                    po.date_from <= now() and
                    (po.date_to is null or po.date_to >= now())
         left join specialization on specialization.po = po.id
order by sp_id, po_id, degree_id;

-- module_view

create
materialized
view module_view as
select module.id                                as id,
       module.title                             as title,
       module.abbrev                            as abbrev,
       module.ects                              as ects,
       identity.id                              as module_management_id,
       identity.kind                            as module_management_kind,
       identity.abbreviation                    as module_management_abbreviation,
       identity.title                           as module_management_title,
       identity.firstname                       as module_management_firstname,
       identity.lastname                        as module_management_lastname,
       study_program_view.sp_de_label,
       study_program_view.sp_en_label,
       study_program_view.sp_id,
       study_program_view.degree_id,
       study_program_view.degree_de_label,
       study_program_view.degree_en_label,
       study_program_view.degree_de_desc,
       study_program_view.degree_en_desc,
       study_program_view.po_id,
       study_program_view.po_version,
       study_program_view.spec_label,
       study_program_view.spec_id,
       module_po_mandatory.recommended_semester as recommended_semester,
       true                                     as mandatory,
       false                                    as focus
from module
         join module_responsibility on module.id = module_responsibility.module and
                                       module_responsibility.responsibility_type = 'module_management'
         join identity on module_responsibility.identity = identity.id
         join module_po_mandatory on module.id = module_po_mandatory.module
         join study_program_view on study_program_view.po_id = module_po_mandatory.po
union
select module.id                               as id,
       module.title                            as title,
       module.abbrev                           as abbrev,
       module.ects                             as ects,
       identity.id                             as module_management_id,
       identity.kind                           as module_management_kind,
       identity.abbreviation                   as module_management_abbreviation,
       identity.title                          as module_management_title,
       identity.firstname                      as module_management_firstname,
       identity.lastname                       as module_management_lastname,
       study_program_view.sp_de_label,
       study_program_view.sp_en_label,
       study_program_view.sp_id,
       study_program_view.degree_id,
       study_program_view.degree_de_label,
       study_program_view.degree_en_label,
       study_program_view.degree_de_desc,
       study_program_view.degree_en_desc,
       study_program_view.po_id,
       study_program_view.po_version,
       study_program_view.spec_label,
       study_program_view.spec_id,
       module_po_optional.recommended_semester as recommended_semester,
       false                                   as mandatory,
       module_po_optional.focus                as focus
from module
         join module_responsibility on module.id = module_responsibility.module and
                                       module_responsibility.responsibility_type = 'module_management'
         join identity on module_responsibility.identity = identity.id
         join module_po_optional on module.id = module_po_optional.module
         join study_program_view on study_program_view.po_id = module_po_optional.po;

-- !Downs
drop
materialized view module_view;
drop
materialized view study_program_view;
drop table module_catalog_generation_request if exists;
drop table module_catalog if exists;
drop table module_review if exists;
drop table module_update_permission if exists;
drop table module_draft if exists;
drop table module_taught_with if exists;
drop table module_global_criteria if exists;
drop table module_competence if exists;
drop table module_po_optional if exists;
drop table module_po_mandatory if exists;
drop table prerequisites_po if exists;
drop table prerequisites_module if exists;
drop table module_prerequisites if exists;
drop table module_assessment_method_precondition if exists;
drop table module_assessment_method if exists;
drop table module_responsibility if exists;
drop table module_relation if exists;
drop table module_ects_focus_area_contribution if exists;
drop table module if exists;
drop table specialization if exists;
drop table po_modification_date if exists;
drop table po if exists;
drop table focus_area if exists;
drop table study_program_person if exists;
drop table study_program if exists;
drop table person_in_faculty if exists;
drop table identity if exists;
drop table global_criteria if exists;
drop table degree if exists;
drop table faculty if exists;
drop table competence if exists;
drop table season if exists;
drop table module_type if exists;
drop table assessment_method if exists;
drop table status if exists;
drop table language if exists;
drop table location if exists;