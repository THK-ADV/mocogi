-- drop
drop materialized view if exists study_program_view cascade;

drop materialized view if exists module_view cascade;

-- create
create materialized view study_program_view as
with
  data as (
    select
      study_program.de_label as sp_de_label,
      study_program.en_label as sp_en_label,
      study_program.id as sp_id,
      study_program.abbreviation as sp_abbrev,
      degree.id as degree_id,
      degree.de_label as degree_de_label,
      degree.en_label as degree_en_label,
      degree.de_desc as degree_de_desc,
      degree.en_desc as degree_en_desc,
      po.id as po_id,
      po.version as po_version,
      null as spec_label,
      null as spec_id
    from
      study_program
      join degree on study_program.degree = degree.id
      join po on po.study_program = study_program.id
      and (
        po.date_to is null
        or po.date_to >= now()
      )
    union
    select
      study_program.de_label as sp_de_label,
      study_program.en_label as sp_en_label,
      study_program.id as sp_id,
      study_program.abbreviation as sp_abbrev,
      degree.id as degree_id,
      degree.de_label as degree_de_label,
      degree.en_label as degree_en_label,
      degree.de_desc as degree_de_desc,
      degree.en_desc as degree_en_desc,
      po.id as po_id,
      po.version as po_version,
      specialization.label as spec_label,
      specialization.id as spec_id
    from
      study_program
      join degree on study_program.degree = degree.id
      join po on po.study_program = study_program.id
      and (
        po.date_to is null
        or po.date_to >= now()
      )
      join specialization on specialization.po = po.id
  )
select
    *
from
    data
order by
    data.sp_id,
    data.po_id,
    data.degree_id,
    data.spec_id;

create materialized view module_view as
select
    module.id as id,
    module.title as title,
    module.abbrev as abbrev,
    module.ects as ects,
    identity.id as module_management_id,
    identity.kind as module_management_kind,
    identity.abbreviation as module_management_abbreviation,
    identity.title as module_management_title,
    identity.firstname as module_management_firstname,
    identity.lastname as module_management_lastname,
    study_program_view.sp_de_label,
    study_program_view.sp_en_label,
    study_program_view.sp_abbrev,
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
    true as mandatory
from
    module
        join module_responsibility on module.id = module_responsibility.module
        and module_responsibility.responsibility_type = 'module_management'
        join identity on module_responsibility.identity = identity.id
        join module_po_mandatory on module.id = module_po_mandatory.module
        join study_program_view on study_program_view.po_id = module_po_mandatory.po
        and case
                when module_po_mandatory.specialization is not null then study_program_view.spec_id = module_po_mandatory.specialization
                else true
                                       end
union
select
    module.id as id,
    module.title as title,
    module.abbrev as abbrev,
    module.ects as ects,
    identity.id as module_management_id,
    identity.kind as module_management_kind,
    identity.abbreviation as module_management_abbreviation,
    identity.title as module_management_title,
    identity.firstname as module_management_firstname,
    identity.lastname as module_management_lastname,
    study_program_view.sp_de_label,
    study_program_view.sp_en_label,
    study_program_view.sp_abbrev,
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
    false as mandatory
from
    module
        join module_responsibility on module.id = module_responsibility.module
        and module_responsibility.responsibility_type = 'module_management'
        join identity on module_responsibility.identity = identity.id
        join module_po_optional on module.id = module_po_optional.module
        join study_program_view on study_program_view.po_id = module_po_optional.po
        and case
                when module_po_optional.specialization is not null then study_program_view.spec_id = module_po_optional.specialization
                else true
                                       end;

-- refresh
refresh materialized view study_program_view;

refresh materialized view module_view;
