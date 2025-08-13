-- drop
DROP MATERIALIZED VIEW IF EXISTS study_program_view CASCADE;

DROP MATERIALIZED VIEW IF EXISTS module_view CASCADE;

DROP VIEW IF EXISTS study_program_view_not_expired;

DROP VIEW IF EXISTS study_program_view_currently_active;

-- create
CREATE MATERIALIZED VIEW study_program_view AS
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
        study_program
        JOIN degree ON study_program.degree = degree.id
        JOIN po ON po.study_program = study_program.id
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
    study_program
    JOIN degree ON study_program.degree = degree.id
    JOIN po ON po.study_program = study_program.id
    JOIN specialization ON specialization.po = po.id
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

CREATE VIEW study_program_view_not_expired AS
SELECT
    *
FROM
    study_program_view
WHERE
    date_to IS NULL
    OR date_to >= now();

CREATE VIEW study_program_view_currently_active AS
SELECT
    *
FROM
    study_program_view
WHERE
    date_from <= now()
    AND (date_to IS NULL
        OR date_to >= now());

CREATE MATERIALIZED VIEW module_view AS
SELECT
    module.id AS id,
    module.title AS title,
    module.abbrev AS abbrev,
    module.ects AS ects,
    module.status AS status,
    identity.id AS module_management_id,
    identity.kind AS module_management_kind,
    identity.abbreviation AS module_management_abbreviation,
    identity.title AS module_management_title,
    identity.firstname AS module_management_firstname,
    identity.lastname AS module_management_lastname,
    study_program_view_not_expired.sp_de_label,
    study_program_view_not_expired.sp_en_label,
    study_program_view_not_expired.sp_abbrev,
    study_program_view_not_expired.sp_id,
    study_program_view_not_expired.degree_id,
    study_program_view_not_expired.degree_de_label,
    study_program_view_not_expired.degree_en_label,
    study_program_view_not_expired.degree_de_desc,
    study_program_view_not_expired.degree_en_desc,
    study_program_view_not_expired.po_id,
    study_program_view_not_expired.po_version,
    study_program_view_not_expired.spec_label,
    study_program_view_not_expired.spec_id,
    module_po_mandatory.recommended_semester AS recommended_semester,
    TRUE AS mandatory
FROM
    module
    JOIN module_responsibility ON module.id = module_responsibility.module
        AND module_responsibility.responsibility_type = 'module_management'
    JOIN IDENTITY ON module_responsibility.identity = identity.id
    JOIN module_po_mandatory ON module.id = module_po_mandatory.module
    JOIN study_program_view_not_expired ON study_program_view_not_expired.po_id = module_po_mandatory.po
        AND CASE WHEN module_po_mandatory.specialization IS NOT NULL THEN
            study_program_view_not_expired.spec_id = module_po_mandatory.specialization
        ELSE
            TRUE
        END
    UNION
    SELECT
        module.id AS id,
        module.title AS title,
        module.abbrev AS abbrev,
        module.ects AS ects,
        module.status AS status,
        identity.id AS module_management_id,
        identity.kind AS module_management_kind,
        identity.abbreviation AS module_management_abbreviation,
        identity.title AS module_management_title,
        identity.firstname AS module_management_firstname,
        identity.lastname AS module_management_lastname,
        study_program_view_not_expired.sp_de_label,
        study_program_view_not_expired.sp_en_label,
        study_program_view_not_expired.sp_abbrev,
        study_program_view_not_expired.sp_id,
        study_program_view_not_expired.degree_id,
        study_program_view_not_expired.degree_de_label,
        study_program_view_not_expired.degree_en_label,
        study_program_view_not_expired.degree_de_desc,
        study_program_view_not_expired.degree_en_desc,
        study_program_view_not_expired.po_id,
        study_program_view_not_expired.po_version,
        study_program_view_not_expired.spec_label,
        study_program_view_not_expired.spec_id,
        module_po_optional.recommended_semester AS recommended_semester,
        FALSE AS mandatory
    FROM
        module
    JOIN module_responsibility ON module.id = module_responsibility.module
        AND module_responsibility.responsibility_type = 'module_management'
    JOIN IDENTITY ON module_responsibility.identity = identity.id
    JOIN module_po_optional ON module.id = module_po_optional.module
    JOIN study_program_view_not_expired ON study_program_view_not_expired.po_id = module_po_optional.po
        AND CASE WHEN module_po_optional.specialization IS NOT NULL THEN
            study_program_view_not_expired.spec_id = module_po_optional.specialization
        ELSE
            TRUE
        END;

-- refresh
REFRESH MATERIALIZED VIEW study_program_view;

REFRESH MATERIALIZED VIEW module_view;

CREATE OR REPLACE VIEW module_core AS
SELECT
    jsonb_agg(module_json ORDER BY title) AS modules
FROM (
    -- Live modules
    SELECT
        m.title,
        jsonb_build_object('id', m.id, 'title', m.title, 'abbreviation', m.abbrev, 'moduleManagement', coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'lastname', i.lastname, 'firstname', i.firstname)) FILTER (WHERE mr.responsibility_type = 'module_management'), '[]'::jsonb), 'ects', m.ects, 'isLive', TRUE) AS module_json
    FROM
        module m
    LEFT JOIN module_responsibility mr ON m.id = mr.module
        AND mr.responsibility_type = 'module_management'
    LEFT JOIN IDENTITY i ON mr.identity = i.id
GROUP BY
    m.id
UNION ALL
-- Draft modules
SELECT
    cmd.module_title AS title,
    jsonb_build_object('id', cmd.module, 'title', cmd.module_title, 'abbreviation', cmd.module_abbrev, 'moduleManagement', coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'lastname', i.lastname, 'firstname', i.firstname)) FILTER (WHERE i.id IS NOT NULL), '[]'::jsonb), 'ects', cmd.module_ects, 'isLive', FALSE) AS module_json
FROM
    created_module_in_draft cmd
    LEFT JOIN LATERAL unnest(cmd.module_management) AS mgmt_id ON TRUE
    LEFT JOIN IDENTITY i ON i.id = mgmt_id
GROUP BY
    cmd.module) subquery;

