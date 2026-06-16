DROP MATERIALIZED VIEW IF EXISTS core.study_program_view CASCADE;

DROP MATERIALIZED VIEW IF EXISTS modules.module_view CASCADE;

DROP VIEW IF EXISTS core.study_program_view_not_expired;

DROP VIEW IF EXISTS core.study_program_view_currently_active;

-- Denormalizes study programs, degrees, POs, and optional specializations into a
-- single PO-centric row set. Downstream views and module lookups use it as the
-- canonical source for study-program context.
CREATE MATERIALIZED VIEW core.study_program_view AS
with data AS (
  SELECT
    study_program.de_label AS sp_de_label,
    study_program.en_label AS sp_en_label,
    study_program.id AS sp_id,
    study_program.abbreviation AS sp_abbrev,
    degree.id AS degree_id,
    degree.de_label AS degree_de_label,
    degree.en_label AS degree_en_label,
    degree.de_desc AS degree_de_desc,
    degree.en_desc AS degree_en_desc,
    po.id AS po_id,
    po.version AS po_version,
    po.date_from AS date_from,
    po.date_to AS date_to,
    NULL AS spec_label,
    NULL AS spec_id
  FROM
    core.study_program
    JOIN core.degree ON study_program.degree = degree.id
    JOIN core.po ON po.study_program = study_program.id
UNION
SELECT
  study_program.de_label AS sp_de_label,
  study_program.en_label AS sp_en_label,
  study_program.id AS sp_id,
  study_program.abbreviation AS sp_abbrev,
  degree.id AS degree_id,
  degree.de_label AS degree_de_label,
  degree.en_label AS degree_en_label,
  degree.de_desc AS degree_de_desc,
  degree.en_desc AS degree_en_desc,
  po.id AS po_id,
  po.version AS po_version,
  po.date_from AS date_from,
  po.date_to AS date_to,
  specialization.label AS spec_label,
  specialization.id AS spec_id
FROM
  core.study_program
  JOIN core.degree ON study_program.degree = degree.id
  JOIN core.po ON po.study_program = study_program.id
  JOIN core.specialization ON specialization.po = po.id
)
SELECT
  *
FROM
  data
ORDER BY
  data.sp_id,
  data.po_id,
  data.degree_id,
  data.spec_id;

-- Filters `study_program_view` down to regulations that are still valid today.
-- Future POs remain visible here so callers can prepare upcoming regulations
-- without seeing expired ones.
CREATE VIEW core.study_program_view_not_expired AS
SELECT
  *
FROM
  core.study_program_view
WHERE
  date_to IS NULL
  OR date_to >= now();

-- Filters `study_program_view` to regulations that are active right now.
-- Use this when callers should ignore both expired and not-yet-active POs.
CREATE VIEW core.study_program_view_currently_active AS
SELECT
  *
FROM
  core.study_program_view
WHERE
  date_from <= now()
  AND (date_to IS NULL
    OR date_to >= now());

-- Pre-aggregates the extended module API shape into one row per module with JSON
-- arrays for module management and study-program associations. This keeps the
-- expensive row explosion in SQL instead of rebuilding the shape in Scala.
CREATE MATERIALIZED VIEW modules.module_view AS
WITH module_management_rows AS (
  SELECT DISTINCT
    module_responsibility.module AS module_id,
    identity.id AS management_id,
    jsonb_build_object('id', identity.id, 'abbreviation', coalesce(identity.abbreviation, ''), 'kind', identity.kind, 'title', identity.title, 'firstname', coalesce(identity.firstname, ''), 'lastname', coalesce(identity.lastname, '')) AS management_json
  FROM
    modules.module_responsibility
    JOIN core.identity ON module_responsibility.identity = identity.id
  WHERE
    module_responsibility.responsibility_type = 'module_management'
),
module_management_agg AS (
  SELECT
    module_id,
    jsonb_agg(management_json) AS "moduleManagement"
  FROM
    module_management_rows
  GROUP BY
    module_id
),
study_program_rows AS (
  SELECT
    module_po_mandatory.module AS module_id,
    study_program_view_not_expired.sp_id,
    study_program_view_not_expired.po_id,
    study_program_view_not_expired.po_version,
    study_program_view_not_expired.spec_id,
    TRUE AS mandatory,
    module_po_mandatory.recommended_semester,
    jsonb_build_object('studyProgram', jsonb_build_object('id', study_program_view_not_expired.sp_id, 'deLabel', study_program_view_not_expired.sp_de_label, 'enLabel', study_program_view_not_expired.sp_en_label, 'abbreviation', study_program_view_not_expired.sp_abbrev, 'po', jsonb_build_object('id', study_program_view_not_expired.po_id, 'version', study_program_view_not_expired.po_version), 'degree', jsonb_build_object('id', study_program_view_not_expired.degree_id, 'deLabel', study_program_view_not_expired.degree_de_label, 'deDesc', study_program_view_not_expired.degree_de_desc, 'enLabel', study_program_view_not_expired.degree_en_label, 'enDesc', study_program_view_not_expired.degree_en_desc), 'specialization', CASE WHEN study_program_view_not_expired.spec_id IS NULL THEN
          'null'::jsonb
        ELSE
          jsonb_build_object('id', study_program_view_not_expired.spec_id, 'deLabel', study_program_view_not_expired.spec_label, 'enLabel', study_program_view_not_expired.spec_label)
        END), 'mandatory', TRUE, 'recommendedSemester', to_jsonb(module_po_mandatory.recommended_semester)) AS study_program_json
  FROM
    modules.module_po_mandatory
    JOIN core.study_program_view_not_expired ON core.study_program_view_not_expired.po_id = modules.module_po_mandatory.po
      AND CASE WHEN module_po_mandatory.specialization IS NOT NULL THEN
        core.study_program_view_not_expired.spec_id = modules.module_po_mandatory.specialization
      ELSE
        TRUE
      END
    UNION ALL
    SELECT
      module_po_optional.module AS module_id,
      study_program_view_not_expired.sp_id,
      study_program_view_not_expired.po_id,
      study_program_view_not_expired.po_version,
      study_program_view_not_expired.spec_id,
      FALSE AS mandatory,
      module_po_optional.recommended_semester,
      jsonb_build_object('studyProgram', jsonb_build_object('id', study_program_view_not_expired.sp_id, 'deLabel', study_program_view_not_expired.sp_de_label, 'enLabel', study_program_view_not_expired.sp_en_label, 'abbreviation', study_program_view_not_expired.sp_abbrev, 'po', jsonb_build_object('id', study_program_view_not_expired.po_id, 'version', study_program_view_not_expired.po_version), 'degree', jsonb_build_object('id', study_program_view_not_expired.degree_id, 'deLabel', study_program_view_not_expired.degree_de_label, 'deDesc', study_program_view_not_expired.degree_de_desc, 'enLabel', study_program_view_not_expired.degree_en_label, 'enDesc', study_program_view_not_expired.degree_en_desc), 'specialization', CASE WHEN study_program_view_not_expired.spec_id IS NULL THEN
            'null'::jsonb
          ELSE
            jsonb_build_object('id', study_program_view_not_expired.spec_id, 'deLabel', study_program_view_not_expired.spec_label, 'enLabel', study_program_view_not_expired.spec_label)
          END), 'mandatory', FALSE, 'recommendedSemester', to_jsonb(module_po_optional.recommended_semester)) AS study_program_json
    FROM
      modules.module_po_optional
    JOIN core.study_program_view_not_expired ON core.study_program_view_not_expired.po_id = modules.module_po_optional.po
      AND CASE WHEN module_po_optional.specialization IS NOT NULL THEN
        core.study_program_view_not_expired.spec_id = modules.module_po_optional.specialization
      ELSE
        TRUE
      END
),
study_program_agg AS (
  SELECT
    module_id,
    jsonb_agg(study_program_json) AS "studyProgram"
  FROM ( SELECT DISTINCT
      module_id,
      study_program_json
    FROM
      study_program_rows) deduped_study_program_rows
GROUP BY
  module_id
)
SELECT
  module.id AS id,
  module.title AS title,
  module.abbrev AS abbrev,
  module.ects AS ects,
  module.status AS status,
  module_management_agg."moduleManagement",
  study_program_agg."studyProgram"
FROM
  modules.module
  JOIN module_management_agg ON module_management_agg.module_id = module.id
  JOIN study_program_agg ON study_program_agg.module_id = module.id;

REFRESH MATERIALIZED VIEW core.study_program_view;

REFRESH MATERIALIZED VIEW modules.module_view;
