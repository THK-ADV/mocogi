-- !Ups

-- adds module type column
alter table created_module_in_draft
    add column "module_type"          varchar(20)   not null default 'module',
    add column "module_mandatory_pos" varchar(32)[] not null default '{}',
    add column "module_optional_pos"  varchar(32)[] not null default '{}';
