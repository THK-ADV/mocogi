-- !Ups

-- Create modules schema
CREATE SCHEMA IF NOT EXISTS modules;
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS schedule;

-- Core Tables
ALTER TABLE public.degree SET SCHEMA core;
ALTER TABLE public.identity SET SCHEMA core;
ALTER TABLE public.language SET SCHEMA core;
ALTER TABLE public.location SET SCHEMA core;
ALTER TABLE public.module_type SET SCHEMA core;
ALTER TABLE public.po SET SCHEMA core;
ALTER TABLE public.season SET SCHEMA core;
ALTER TABLE public.specialization SET SCHEMA core;
ALTER TABLE public.status SET SCHEMA core;
ALTER TABLE public.assessment_method SET SCHEMA core;
ALTER TABLE public.study_program SET SCHEMA core;
ALTER TABLE public.study_program_person SET SCHEMA core;
ALTER TABLE public.people_images SET SCHEMA core;

ALTER MATERIALIZED VIEW public.study_program_view SET SCHEMA core;
ALTER VIEW public.study_program_view_not_expired SET SCHEMA core;
ALTER VIEW public.study_program_view_currently_active SET SCHEMA core;

-- Modules Tables
ALTER TABLE public.created_module_in_draft SET SCHEMA modules;
ALTER TABLE public.exam_list SET SCHEMA modules;
ALTER TABLE public.module SET SCHEMA modules;
ALTER TABLE public.module_assessment_method SET SCHEMA modules;
ALTER TABLE public.module_companion SET SCHEMA modules;
ALTER TABLE public.module_draft SET SCHEMA modules;
ALTER TABLE public.module_po_mandatory SET SCHEMA modules;
ALTER TABLE public.module_po_optional SET SCHEMA modules;
ALTER TABLE public.module_relation SET SCHEMA modules;
ALTER TABLE public.module_responsibility SET SCHEMA modules;
ALTER TABLE public.module_review SET SCHEMA modules;
ALTER TABLE public.module_taught_with SET SCHEMA modules;
ALTER TABLE public.module_update_permission SET SCHEMA modules;
ALTER TABLE public.permission SET SCHEMA modules;
ALTER TABLE public.permitted_assessment_method_for_module SET SCHEMA modules;

ALTER MATERIALIZED VIEW public.module_view SET SCHEMA modules;
ALTER VIEW public.module_core SET SCHEMA modules;

-- !Downs

DROP SCHEMA IF EXISTS schedule CASCADE;
DROP SCHEMA IF EXISTS modules CASCADE;
DROP SCHEMA IF EXISTS core CASCADE;

-- Move everything back to public

ALTER VIEW modules.module_core SET SCHEMA public;
ALTER MATERIALIZED VIEW modules.module_view SET SCHEMA public;

ALTER TABLE modules.permission SET SCHEMA public;
ALTER TABLE modules.permitted_assessment_method_for_module SET SCHEMA public;
ALTER TABLE modules.module_update_permission SET SCHEMA public;
ALTER TABLE modules.module_taught_with SET SCHEMA public;
ALTER TABLE modules.module_review SET SCHEMA public;
ALTER TABLE modules.module_responsibility SET SCHEMA public;
ALTER TABLE modules.module_relation SET SCHEMA public;
ALTER TABLE modules.module_po_optional SET SCHEMA public;
ALTER TABLE modules.module_po_mandatory SET SCHEMA public;
ALTER TABLE modules.module_draft SET SCHEMA public;
ALTER TABLE modules.module_companion SET SCHEMA public;
ALTER TABLE modules.module_assessment_method SET SCHEMA public;
ALTER TABLE modules.module SET SCHEMA public;
ALTER TABLE modules.exam_list SET SCHEMA public;
ALTER TABLE modules.created_module_in_draft SET SCHEMA public;

ALTER VIEW core.study_program_view_currently_active SET SCHEMA public;
ALTER VIEW core.study_program_view_not_expired SET SCHEMA public;
ALTER MATERIALIZED VIEW core.study_program_view SET SCHEMA public;

ALTER TABLE core.people_images SET SCHEMA public;
ALTER TABLE core.study_program_person SET SCHEMA public;
ALTER TABLE core.study_program SET SCHEMA public;
ALTER TABLE core.assessment_method SET SCHEMA public;
ALTER TABLE core.status SET SCHEMA public;
ALTER TABLE core.specialization SET SCHEMA public;
ALTER TABLE core.season SET SCHEMA public;
ALTER TABLE core.po SET SCHEMA public;
ALTER TABLE core.module_type SET SCHEMA public;
ALTER TABLE core.location SET SCHEMA public;
ALTER TABLE core.language SET SCHEMA public;
ALTER TABLE core.identity SET SCHEMA public;
ALTER TABLE core.degree SET SCHEMA public;
