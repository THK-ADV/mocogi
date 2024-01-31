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

create table grade
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "de_desc"  text not null,
    "en_label" text not null,
    "en_desc"  text not null
);

create table study_form_type
(
    "id"       text PRIMARY KEY,
    "de_label" text not null,
    "en_label" text not null
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
    "id"                             text PRIMARY KEY,
    "de_label"                       text    not null,
    "en_label"                       text    not null,
    "internal_abbreviation"          text    not null,
    "external_abbreviation"          text    not null,
    "de_url"                         text    not null,
    "en_url"                         text    not null,
    "grade"                          text    not null,
    "accreditation_until"            date    not null,
    "restricted_admission_value"     boolean not null,
    "restricted_admission_de_reason" text    not null,
    "restricted_admission_en_reason" text    not null,
    "de_description"                 text    not null,
    "de_note"                        text    not null,
    "en_description"                 text    not null,
    "en_note"                        text    not null,
    FOREIGN KEY (grade) REFERENCES grade (id)
);

create table study_form
(
    "id"                uuid PRIMARY KEY,
    "study_program"     text     not null,
    "study_form_type"   text     not null,
    "workload_per_ects" smallint not null,
    FOREIGN KEY (study_program) REFERENCES study_program (id),
    FOREIGN KEY (study_form_type) REFERENCES study_form_type (id)
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
    FOREIGN KEY (language) REFERENCES language (id),
    FOREIGN KEY (study_program) REFERENCES study_program (id)
);

create table study_program_season
(
    "season"        text not null,
    "study_program" text not null,
    PRIMARY KEY (season, study_program),
    FOREIGN KEY (season) REFERENCES season (id),
    FOREIGN KEY (study_program) REFERENCES study_program (id)
);

create table study_program_location
(
    "location"      text not null,
    "study_program" text not null,
    PRIMARY KEY (location, study_program),
    FOREIGN KEY (location) REFERENCES location (id),
    FOREIGN KEY (study_program) REFERENCES study_program (id)
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
    "date"          date     not null,
    "date_from"     date     not null,
    "date_to"       date,
    FOREIGN KEY (study_program) REFERENCES study_program (id)
);

create table po_modification_date
(
    "po"   text not null,
    "date" date not null,
    PRIMARY KEY (po, date),
    FOREIGN KEY (po) REFERENCES po (id)
);

create table specialization
(
    "id"    text PRIMARY KEY,
    "po"    text not null,
    "label" text not null,
    FOREIGN KEY (po) REFERENCES po (id)
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
    FOREIGN KEY (module_type) REFERENCES module_type (id),
    FOREIGN KEY (language) REFERENCES language (id),
    FOREIGN KEY (season) REFERENCES season (id),
    FOREIGN KEY (status) REFERENCES status (id),
    FOREIGN KEY (location) REFERENCES location (id)
);

create table ects_focus_area_contribution
(
    "focus_area" text          not null,
    "metadata"   uuid          not null,
    "ects_value" numeric(4, 2) not null,
    "de_desc"    text          not null,
    "en_desc"    text          not null,
    PRIMARY KEY (focus_area, metadata),
    FOREIGN KEY (focus_area) REFERENCES focus_area (id),
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
    "identity"            text not null,
    "responsibility_type" text not null,
    PRIMARY KEY (metadata, identity, responsibility_type),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (identity) REFERENCES identity (id)
);

create table metadata_assessment_method
(
    "id"                     uuid not null PRIMARY KEY,
    "metadata"               uuid not null,
    "assessment_method"      text not null,
    "assessment_method_type" text not null,
    "percentage"             numeric(5, 2) null,
    FOREIGN KEY (assessment_method) REFERENCES assessment_method (id),
    FOREIGN KEY (metadata) REFERENCES metadata (id)
);

create table metadata_assessment_method_precondition
(
    "assessment_method"          text not null,
    "metadata_assessment_method" uuid not null,
    PRIMARY KEY (assessment_method, metadata_assessment_method),
    FOREIGN KEY (assessment_method) REFERENCES assessment_method (id),
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
    FOREIGN KEY (po) REFERENCES po (id)
);

create table po_mandatory
(
    "id"                             uuid not null PRIMARY KEY,
    "metadata"                       uuid not null,
    "po"                             text not null,
    "recommended_semester"           text not null,
    "recommended_semester_part_time" text not null,
    "specialization"                 text null,
    FOREIGN KEY (specialization) REFERENCES specialization (id),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (po) REFERENCES po (id)
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
    FOREIGN KEY (specialization) REFERENCES specialization (id),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (po) REFERENCES po (id),
    FOREIGN KEY (instance_of) REFERENCES metadata (id)
);

create table metadata_competence
(
    "metadata"   uuid not null,
    "competence" text not null,
    PRIMARY KEY (metadata, competence),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (competence) REFERENCES competence (id)
);

create table metadata_global_criteria
(
    "metadata"        uuid not null,
    "global_criteria" text not null,
    PRIMARY KEY (metadata, global_criteria),
    FOREIGN KEY (metadata) REFERENCES metadata (id),
    FOREIGN KEY (global_criteria) REFERENCES global_criteria (id)
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

create table module_draft
(
    "module"                  uuid      not null PRIMARY KEY,
    "module_title"            text      not null,
    "module_abbrev"           text      not null,
    "author"                  text      not null,
    "branch"                  text      not null,
    "source"                  text      not null,
    "module_json"             text      not null,
    "module_compendium_json"  text      not null,
    "module_compendium_print" text      not null,
    "keys_to_be_reviewed"     text      not null,
    "modified_keys"           text      not null,
    "last_commit_id"          text null,
    "merge_request_id"        integer null,
    "merge_request_status"    text null,
    "last_modified"           timestamp not null,
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

-- module compendium list
create table module_compendium_list
(
    "full_po"        text      not null PRIMARY KEY,
    "po"             text      not null,
    "po_number"      smallint  not null,
    "specialization" text null,
    "study_program"  text      not null,
    "semester"       text      not null,
    "de_url"         text      not null,
    "en_url"         text      not null,
    "generated"      timestamp not null,
    FOREIGN KEY (study_program) REFERENCES study_program (id),
    FOREIGN KEY (po) REFERENCES po (id)
);

-- study_program_view
create
materialized view study_program_view as
select study_program.de_label as sp_label,
       study_program.id       as sp_id,
       grade.de_label         as grade_label,
       po.id                  as po_id,
       po.version             as po_version,
       specialization.label   as spec_label,
       specialization.id      as spec_id
from study_program
         join grade on study_program.grade = grade.id
         join po on po.study_program = study_program.id and
                    po.date_from <= now() and
                    (po.date_to is null or po.date_to >= now())
         left join specialization on specialization.po = po.id
order by sp_label, po_id, grade_label;

-- module_view
create
materialized view module_view as
select metadata.id                       as id,
       metadata.title                    as title,
       metadata.abbrev                   as abbrev,
       metadata.ects                     as ects,
       identity.id                       as module_management_id,
       identity.kind                     as module_management_kind,
       identity.abbreviation             as module_management_abbrev,
       identity.title                    as module_management_title,
       identity.firstname                as module_management_firstname,
       identity.lastname                 as module_management_lastname,
       po.id                             as po_id,
       po.version                        as po_version,
       study_program.id                  as sp_id,
       study_program.de_label            as sp_label,
       grade.de_label                    as grade_label,
       specialization.id                 as spec_id,
       specialization.label              as spec_label,
       po_mandatory.recommended_semester as recommended_semester,
       true                              as mandatory
from metadata
         join responsibility on metadata.id = responsibility.metadata and
                                responsibility.responsibility_type = 'module_management'
         join identity on responsibility.identity = identity.id
         join po_mandatory on metadata.id = po_mandatory.metadata
         join po on po_mandatory.po = po.id
         join study_program on po.study_program = study_program.id
         join grade on study_program.grade = grade.id
         left join specialization on po.id = specialization.po and
                                     po_mandatory.specialization = specialization.id
union
select metadata.id                       as id,
       metadata.title                    as title,
       metadata.abbrev                   as abbrev,
       metadata.ects                     as ects,
       identity.id                       as module_management_id,
       identity.kind                     as module_management_kind,
       identity.abbreviation             as module_management_abbrev,
       identity.title                    as module_management_title,
       identity.firstname                as module_management_firstname,
       identity.lastname                 as module_management_lastname,
       po.id                             as po_id,
       po.version                        as po_version,
       study_program.id                  as sp_id,
       study_program.de_label            as sp_label,
       grade.de_label                    as grade_label,
       specialization.id                 as spec_id,
       specialization.label              as spec_label,
       po_mandatory.recommended_semester as recommended_semester,
       false                             as mandatory
from metadata
         join responsibility on metadata.id = responsibility.metadata and
                                responsibility.responsibility_type = 'module_management'
         join identity on responsibility.identity = identity.id
         join po_optional on metadata.id = po_optional.metadata
         join po on po_optional.po = po.id
         join study_program on po.study_program = study_program.id
         join grade on study_program.grade = grade.id
         left join specialization on po.id = specialization.po and
                                     po_optional.specialization = specialization.id;

-- !Downs
drop
materialized view module_view;
drop
materialized view study_program_view;
drop table module_compendium_list if exists;
drop table module_review if exists;
drop table module_update_permission if exists;
drop table module_draft if exists;
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
drop table study_program_person if exists;
drop table study_program_location if exists;
drop table study_program_season if exists;
drop table study_program_language if exists;
drop table study_form_scope if exists;
drop table study_form if exists;
drop table study_program if exists;
drop table person_in_faculty if exists;
drop table identity if exists;
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