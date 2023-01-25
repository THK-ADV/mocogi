-- !Ups

alter table metadata
    add column learning_outcome_de text not null default '',
    add column learning_outcome_en text not null default '',
    add column module_content_de   text not null default '',
    add column module_content_en   text not null default '',
    add column learning_methods_de text not null default '',
    add column learning_methods_en text not null default '',
    add column literature_de       text not null default '',
    add column literature_en       text not null default '',
    add column particularities_de  text not null default '',
    add column particularities_en  text not null default '';

-- !Downs
alter table metadata
    drop column learning_outcome_de,
    drop column learning_outcome_en,
    drop column module_content_de,
    drop column module_content_en,
    drop column learning_methods_de,
    drop column learning_methods_en,
    drop column literature_de,
    drop column literature_en,
    drop column particularities_de,
    drop column particularities_en;
