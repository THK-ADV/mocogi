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
         join po on po.study_program = study_program.abbrev and
                    po.date_from <= now() and
                    (po.date_to is null or po.date_to >= now())
         left join specialization on specialization.po = po.abbrev
order by sp_label, po_abbrev, grade_label;

-- metadata_atomic
create materialized view module_atomic as
select metadata.id                       as id,
       metadata.title                    as title,
       metadata.abbrev                   as abbrev,
       metadata.ects                     as ects,
       person.id                         as module_management_id,
       person.kind                       as module_management_kind,
       person.abbreviation               as module_management_abbrev,
       person.title                      as module_management_title,
       person.firstname                  as module_management_firstname,
       person.lastname                   as module_management_lastname,
       po.abbrev                         as po_abbrev,
       po.version                        as po_version,
       study_program.abbrev              as sp_abbrev,
       study_program.de_label            as sp_label,
       grade.de_label                    as grade_label,
       specialization.abbrev             as spec_abbrev,
       specialization.label              as spec_label,
       po_mandatory.recommended_semester as recommended_semester,
       true                              as mandatory
from metadata
         join responsibility on metadata.id = responsibility.metadata and
                                responsibility.responsibility_type = 'module_management'
         join person on responsibility.person = person.id
         join po_mandatory on metadata.id = po_mandatory.metadata
         join po on po_mandatory.po = po.abbrev
         join study_program on po.study_program = study_program.abbrev
         join grade on study_program.grade = grade.abbrev
         left join specialization on po.abbrev = specialization.po and
                                     po_mandatory.specialization = specialization.abbrev
union
select metadata.id                      as id,
       metadata.title                   as title,
       metadata.abbrev                  as abbrev,
       metadata.ects                    as ects,
       person.id                        as module_management_id,
       person.kind                      as module_management_kind,
       person.abbreviation              as module_management_abbrev,
       person.title                     as module_management_title,
       person.firstname                 as module_management_firstname,
       person.lastname                  as module_management_lastname,
       po.abbrev                        as po_abbrev,
       po.version                       as po_version,
       study_program.abbrev             as sp_abbrev,
       study_program.de_label           as sp_label,
       grade.de_label                   as grade_label,
       specialization.abbrev            as spec_abbrev,
       specialization.label             as spec_label,
       po_optional.recommended_semester as recommended_semester,
       false                            as mandatory
from metadata
         join responsibility on metadata.id = responsibility.metadata and
                                responsibility.responsibility_type = 'module_management'
         join person on responsibility.person = person.id
         join po_optional on metadata.id = po_optional.metadata
         join po on po_optional.po = po.abbrev
         join study_program on po.study_program = study_program.abbrev
         join grade on study_program.grade = grade.abbrev
         left join specialization on po.abbrev = specialization.po and
                                     po_optional.specialization = specialization.abbrev;