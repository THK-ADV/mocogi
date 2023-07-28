-- study_program_atomic
create materialized view study_program_atomic as
select study_program.de_label as sp_label,
       study_program.abbrev   as sp_abbrev,
       grade.de_label         as grade_label,
       po.abbrev              as po_abbrev,
       po.version             as po_version,
       specialization.label   as spec_label,
       specialization.abbrev  as spec_abbrev
from study_program
         join grade on study_program.grade = grade.abbrev
         join po on po.study_program = study_program.abbrev
         left join specialization on specialization.po = po.abbrev;

-- metadata_atomic
create materialized view metadata_atomic as
select metadata.id                       as id,
       metadata.title                    as title,
       metadata.abbrev                   as abbrev,
       metadata.ects                     as ects,
       person.abbreviation               as module_management_abbrev,
       person.kind                       as module_management_kind,
       person.title                      as module_management_title,
       person.firstname                  as module_management_firstname,
       person.lastname                   as module_management_lastname,
       po.abbrev                         as po_abbrev,
       po.version                        as po_version,
       study_program.de_label            as sp_label,
       grade.de_label                    as grade_label,
       specialization.label              as spec_label,
       po_mandatory.recommended_semester as recommended_semester
from metadata
         join responsibility
              on metadata.id = responsibility.metadata and responsibility.responsibility_type = 'module_management'
         join person on responsibility.person = person.id
         join po_mandatory on metadata.id = po_mandatory.metadata
         join po on po_mandatory.po = po.abbrev
         join study_program on po.study_program = study_program.abbrev
         join grade on study_program.grade = grade.abbrev
         left join specialization
                   on po.abbrev = specialization.po and po_mandatory.specialization = specialization.abbrev;