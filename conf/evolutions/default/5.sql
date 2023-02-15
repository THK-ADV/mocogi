-- !Ups

alter table module_draft
    add column valid_module_compendium_json text null,
    add column module_compendium_print      text null,
    add column pipeline_error               text null;

-- !Downs
alter table module_draft
    drop column valid_module_compendium_json,
    drop column module_compendium_print,
    drop column pipeline_error;
