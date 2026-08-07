DROP FUNCTION IF EXISTS identity_to_json;

DROP FUNCTION IF EXISTS module_to_json_short;

DROP FUNCTION IF EXISTS resolve_prereqs;

DROP FUNCTION IF EXISTS resolve_responsibilities;

DROP FUNCTION IF EXISTS resolve_assessment_methods;

DROP FUNCTION IF EXISTS resolve_po_relationships;

DROP FUNCTION IF EXISTS resolve_taught_with;

DROP FUNCTION IF EXISTS resolve_module_relation;

DROP FUNCTION IF EXISTS get_module_details;

DROP FUNCTION IF EXISTS calculate_module_draft_state;

DROP FUNCTION IF EXISTS build_module_info_json;

DROP FUNCTION IF EXISTS get_modules_for_user;

DROP FUNCTION IF EXISTS get_modules_for_po;

DROP FUNCTION IF EXISTS module_of_po;

DROP FUNCTION IF EXISTS get_user_info;

DROP FUNCTION IF EXISTS get_users_with_granted_permissions_from_module;

DROP FUNCTION IF EXISTS get_generic_module_options;

DROP FUNCTION IF EXISTS generic_modules_for_po;

DROP FUNCTION IF EXISTS schedule.semester_plan_by_now;

DROP VIEW IF EXISTS modules.module_core_raw;

DROP VIEW IF EXISTS modules.module_core;

-- Serializes a core identity into the JSON shape expected by module- and
-- schedule-related APIs. Person identities include the richer person fields,
-- while other identity kinds stay compact.
CREATE OR REPLACE FUNCTION modules.identity_to_json(i core.identity)
  RETURNS jsonb
  LANGUAGE sql
  IMMUTABLE
  AS $$
  SELECT
    CASE WHEN i.kind = 'person' THEN
      jsonb_build_object('id', i.id, 'kind', i.kind, 'title', i.title, 'lastname', i.lastname, 'firstname', i.firstname, 'faculties', i.faculties, 'isActive', i.is_active, 'websiteUrl', i.website_url, 'abbreviation', i.abbreviation, 'employmentType', i.employment_type)
    ELSE
      -- other kinds are 'group' or 'unknown'
      jsonb_build_object('id', i.id, 'title', i.title, 'isActive', i.is_active, 'kind', i.kind)
    END;
$$;

-- Builds the minimal module reference payload used inside larger JSON responses.
CREATE OR REPLACE FUNCTION modules.module_to_json_short(m modules.module)
  RETURNS jsonb
  LANGUAGE sql
  IMMUTABLE
  AS $$
  SELECT
    jsonb_build_object('id', m.id, 'title', m.title, 'abbreviation', m.abbrev);
$$;

-- Expands prerequisite metadata by replacing referenced module ids with compact
-- module objects. This keeps prerequisite payloads readable for detail endpoints.
CREATE OR REPLACE FUNCTION modules.resolve_prereqs(prerequisites jsonb)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    CASE WHEN prerequisites IS NULL THEN
      NULL
    ELSE
      jsonb_build_object('text', prerequisites -> 'text', 'modules', coalesce((
          SELECT
            jsonb_agg(modules.module_to_json_short(module))
          FROM jsonb_array_elements_text(prerequisites -> 'modules') AS arr(mid)
          JOIN modules.module ON module.id = arr.mid::uuid), '[]'::jsonb))
    END;
$$;

-- Aggregates module managers and lecturers for a module, including profile image
-- URLs when available. The result is already shaped for direct JSON embedding.
CREATE OR REPLACE FUNCTION modules.resolve_responsibilities(module_id uuid)
  RETURNS TABLE(
    module_management jsonb,
    lecturer jsonb)
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(jsonb_agg(modules.identity_to_json(i) || jsonb_build_object('imageUrl', pi.image_url)) FILTER(WHERE mr.responsibility_type = 'module_management'), '[]'::jsonb) AS module_management,
    coalesce(jsonb_agg(modules.identity_to_json(i) || jsonb_build_object('imageUrl', pi.image_url)) FILTER(WHERE mr.responsibility_type = 'lecturer'), '[]'::jsonb) AS lecturer
  FROM
    modules.module_responsibility AS mr
    JOIN core.identity AS i ON i.id = mr.identity
    LEFT JOIN core.people_images AS pi ON pi.person = i.id
  WHERE
    mr.module = module_id;
$$;

-- Aggregates assessment methods for a module together with percentages and
-- human-readable precondition labels.
CREATE OR REPLACE FUNCTION modules.resolve_assessment_methods(module_id uuid)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(jsonb_agg(jsonb_build_object('id', am.id, 'label', am.de_label, 'source', am.source, 'percentage', mam.percentage, 'preconditions', coalesce((
            SELECT
              jsonb_agg(pre_am.de_label)
            FROM unnest(mam.precondition) AS pre_id(id)
            JOIN core.assessment_method AS pre_am ON pre_am.id = pre_id.id), '[]'::jsonb))), '[]'::jsonb)
  FROM
    modules.module_assessment_method AS mam
    JOIN core.assessment_method AS am ON am.id = mam.assessment_method
  WHERE
    mam.module = module_id;
$$;

-- Collects the mandatory and optional PO relationships of a module in the JSON
-- format used by the module detail API.
CREATE OR REPLACE FUNCTION modules.resolve_po_relationships(module_id uuid)
  RETURNS TABLE(
    po_mandatory jsonb,
    po_optional jsonb)
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    -- PO Mandatory
    coalesce((
      SELECT
        jsonb_agg(jsonb_build_object('poId', po.id, 'poVersion', po.version, 'poECTSFactor', po.ects_factor, 'studyProgramLabel', sp.de_label, 'studyProgramAbbreviation', sp.abbreviation, 'degree', deg.de_label, 'specializationLabel', spec.label, 'specializationAbbrev', spec.abbreviation, 'recommendedSemester', mpm.recommended_semester, 'studyProgramId', sp.id))
      FROM modules.module_po_mandatory AS mpm
      JOIN core.po ON po.id = mpm.po
      JOIN core.study_program AS sp ON sp.id = po.study_program
      JOIN core.degree AS deg ON deg.id = sp.degree
      LEFT JOIN core.specialization AS spec ON spec.id = mpm.specialization
      WHERE
        mpm.module = module_id), '[]'::jsonb) AS po_mandatory,
    -- PO Optional
    coalesce((
      SELECT
        jsonb_agg(jsonb_build_object('poId', po.id, 'poVersion', po.version, 'poECTSFactor', po.ects_factor, 'studyProgramLabel', sp.de_label, 'studyProgramAbbreviation', sp.abbreviation, 'degree', deg.de_label, 'specializationLabel', spec.label, 'specializationAbbrev', spec.abbreviation, 'recommendedSemester', mpo.recommended_semester, 'instanceOf', modules.module_to_json_short(inst_mod), 'studyProgramId', sp.id))
      FROM modules.module_po_optional AS mpo
      JOIN core.po ON po.id = mpo.po
      JOIN core.study_program AS sp ON sp.id = po.study_program
      JOIN core.degree AS deg ON deg.id = sp.degree
      LEFT JOIN core.specialization AS spec ON spec.id = mpo.specialization
      JOIN modules.module AS inst_mod ON inst_mod.id = mpo.instance_of
      WHERE
        mpo.module = module_id), '[]'::jsonb) AS po_optional;
$$;

-- Returns the modules that are taught together with the given module as compact
-- module references.
CREATE OR REPLACE FUNCTION modules.resolve_taught_with(module_id uuid)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(jsonb_agg(modules.module_to_json_short(m)), '[]'::jsonb)
  FROM
    modules.module_taught_with AS mtw
    JOIN modules.module AS m ON m.id = mtw.module_taught
  WHERE
    mtw.module = module_id;
$$;

-- Resolves the parent/child relationship metadata for a module into the small
-- relation payload consumed by the frontend.
CREATE OR REPLACE FUNCTION modules.resolve_module_relation(module_id uuid)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    CASE WHEN count(*) = 0 THEN
      NULL
    ELSE
      jsonb_build_object('relationType', 'parent', 'modules', jsonb_agg(modules.module_to_json_short(m)
        ORDER BY m.title, m.id))
    END
  FROM
    modules.module_relation AS mr
    JOIN modules.module AS m ON m.id = mr.child
  WHERE
    mr.parent = module_id;
$$;

-- Builds the full module details payload, combining the base module row with all
-- resolved nested metadata needed by the module details page.
-- Note(MD7F2A): Semantics must match Note(MD7F2A) in ModuleDetailRepository.assemble.
CREATE OR REPLACE FUNCTION modules.get_module_details(module_id uuid)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    jsonb_build_object('id', b.id, 'lastModified', b.last_modified, 'title', b.title, 'abbreviation', b.abbrev, 'moduleType', json_build_object('label', b.moduletypelabel, 'id', b.module_type), 'ects', b.ects, 'language', json_build_object('id', b.language, 'label', b.languagelabel), 'duration', b.duration, 'season', b.seasonlabel, 'workload', b.workload, 'status', json_build_object('label', b.statuslabel, 'id', b.status), 'location', b.locationlabel, 'firstExaminer', b.firstexaminer, 'secondExaminer', b.secondexaminer, 'examPhases', b.exam_phases, 'participants', b.participants, 'recommendedPrerequisites', modules.resolve_prereqs(b.recommended_prerequisites), 'requiredPrerequisites', modules.resolve_prereqs(b.required_prerequisites), 'deContent', jsonb_build_object('learningOutcome', b.learning_outcome_de, 'moduleContent', b.module_content_de, 'learningMethods', b.learning_methods_de, 'literature', b.literature_de, 'particularities', b.particularities_de), 'enContent', jsonb_build_object('learningOutcome', b.learning_outcome_en, 'moduleContent', b.module_content_en, 'learningMethods', b.learning_methods_en, 'literature', b.literature_en, 'particularities', b.particularities_en), 'moduleManagement', coalesce(resp.module_management, '[]'::jsonb), 'lecturer', coalesce(resp.lecturer, '[]'::jsonb), 'assessments', modules.resolve_assessment_methods(b.id), 'poMandatory', po.po_mandatory, 'poOptional', po.po_optional, 'taughtWith', modules.resolve_taught_with(b.id), 'moduleRelation', modules.resolve_module_relation(b.id), 'attendanceRequirement', b.attendance_requirement, 'assessmentPrerequisite', b.assessment_prerequisite)
  FROM(
    SELECT
      m.*,
      mt.de_label AS moduletypelabel,
      lng.de_label AS languagelabel,
      ssn.de_label AS seasonlabel,
      sts.de_label AS statuslabel,
      loc.de_label AS locationlabel,
      modules.identity_to_json(fe) AS firstexaminer,
      modules.identity_to_json(se) AS secondexaminer
    FROM
      modules.module AS m
      JOIN core.module_type AS mt ON mt.id = m.module_type
      JOIN core.language AS lng ON lng.id = m.language
      JOIN core.season AS ssn ON ssn.id = m.season
      JOIN core.status AS sts ON sts.id = m.status
      JOIN core.location AS loc ON loc.id = m.location
      JOIN core.identity AS fe ON fe.id = m.first_examiner
      JOIN core.identity AS se ON se.id = m.second_examiner
    WHERE
      m.id = module_id) AS b
  LEFT JOIN LATERAL modules.resolve_responsibilities(b.id) AS resp ON TRUE
  LEFT JOIN LATERAL modules.resolve_po_relationships(b.id) AS po ON TRUE;
$$;

-- Derives the public workflow state of a module draft from its git and review
-- metadata. This keeps state classification centralized in SQL.
CREATE OR REPLACE FUNCTION modules.calculate_module_draft_state(md modules.module_draft)
  RETURNS text
  LANGUAGE sql
  IMMUTABLE
  AS $$
  SELECT
    CASE WHEN md IS NULL THEN
      'published'
    WHEN md.last_commit_id IS NULL THEN
      'unknown'
    WHEN md.merge_request_status IS NULL THEN
      CASE WHEN md.modified_keys != '' and md.keys_to_be_reviewed = '' THEN
        'valid_for_publication'
      WHEN md.modified_keys != '' and md.keys_to_be_reviewed != '' THEN
        'valid_for_review'
      ELSE
        'unknown'
    END
    WHEN md.merge_request_status = 'open' THEN
      CASE WHEN md.keys_to_be_reviewed != '' THEN
        'waiting_for_review'
      WHEN md.keys_to_be_reviewed = '' THEN
        'waiting_for_publication'
      ELSE
        'unknown'
    END
    WHEN md.merge_request_status = 'closed' THEN
      'waiting_for_changes'
    ELSE
      'unknown'
    END
$$;

-- Normalizes live modules, created-in-draft modules, and module drafts into one
-- shared JSON payload used by module list endpoints.
CREATE OR REPLACE FUNCTION modules.build_module_info_json(inherited_perm bool, m modules.module, cm modules.created_module_in_draft, md modules.module_draft, po_mandatory text[], po_optional text[])
  RETURNS jsonb
  LANGUAGE sql
  IMMUTABLE
  AS $$
  SELECT
    -- coalesce with module drafts first (md), because they represent the latest data
    json_build_object('isModuleManager', inherited_perm, 'isNewModule', cm.module IS NOT NULL, 'module', json_build_object('id', coalesce(md.module, cm.module, m.id), 'title', coalesce(md.module_title, cm.module_title, m.title), 'abbreviation', coalesce(md.module_abbrev, cm.module_abbrev, m.abbrev)), 'ects', coalesce((md.module_json -> 'metadata' -> 'ects')::numeric, cm.module_ects, m.ects), 'mandatoryPOs', po_mandatory, 'optionalPOs', po_optional, 'moduleDraftState', modules.calculate_module_draft_state(md), 'moduleDraft', CASE WHEN md.module IS NOT NULL THEN
      json_build_object('id', md.module, 'title', md.module_title, 'abbreviation', md.module_abbrev, 'modifiedKeys', md.modified_keys, 'keysToBeReviewed', md.keys_to_be_reviewed)
    END)::jsonb
$$;

-- Returns the modules visible to a campus user together with permission context,
-- PO coverage, and draft workflow state.
CREATE OR REPLACE FUNCTION modules.get_modules_for_user(campus_id_param text)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(json_agg(modules.build_module_info_json(kind = 'inherited', m, cm, md, po_mandatory, po_optional))::jsonb, '[]'::jsonb)
  FROM(
    -- p.module has always a valid module id which is either linked to m, cm or md
    SELECT DISTINCT ON(p.module)
      p.kind,
      m,
      cm,
      md,
      CASE WHEN cm IS NOT NULL THEN
        cm.module_mandatory_pos
      ELSE
(
          SELECT
            array_agg(DISTINCT mpm.po)
          FROM
            modules.module_po_mandatory mpm
          WHERE
            mpm.module = m.id)
      END AS po_mandatory,
      CASE WHEN cm IS NOT NULL THEN
        cm.module_optional_pos
      ELSE
(
          SELECT
            array_agg(DISTINCT mpo.po)
          FROM
            modules.module_po_optional mpo
          WHERE
            mpo.module = m.id)
      END AS po_optional
    FROM
      modules.module_update_permission p
    LEFT JOIN modules.module m ON p.module = m.id
    LEFT JOIN modules.created_module_in_draft cm ON p.module = cm.module
    LEFT JOIN modules.module_draft md ON p.module = md.module
  WHERE
    p.campus_id = campus_id_param) AS subquery(kind,
    m,
    cm,
    md)
$$;

-- Returns the modules relevant to the requested POs across preview-only drafts,
-- published modules, and published modules with draft metadata. The query
-- precomputes PO matches so callers do not rescan every module per request.
CREATE OR REPLACE FUNCTION modules.get_modules_for_po(pos_param text[])
  RETURNS jsonb STABLE
  LANGUAGE sql
  AS $$
  WITH requested_pos AS(
    SELECT
      po_id
    FROM
      unnest(pos_param) AS po_id
    WHERE
      po_id IS NOT NULL
),
created_modules AS(
  SELECT DISTINCT ON(cm.module)
    NULL::modules.module AS m,
    cm,
    md,
    cm.module_mandatory_pos AS po_mandatory,
    cm.module_optional_pos AS po_optional
  FROM
    requested_pos
    JOIN modules.created_module_in_draft cm ON(EXISTS(
        SELECT
          1
        FROM
          unnest(cm.module_mandatory_pos) AS e(el)
        WHERE
          el LIKE po_id || '%')
        OR EXISTS(
          SELECT
            1
          FROM
            unnest(cm.module_optional_pos) AS e(el)
          WHERE
            el LIKE po_id || '%'))
        LEFT JOIN modules.module_draft md ON md.module = cm.module
),
draft_matching_modules AS(
  SELECT DISTINCT
    md.module
  FROM
    requested_pos
    JOIN modules.module_draft md ON(EXISTS(
        SELECT
          1
        FROM
          jsonb_array_elements(md.module_json -> 'metadata' -> 'po' -> 'mandatory') AS mandatory_po
        WHERE
          mandatory_po ->> 'po' LIKE po_id || '%')
        OR EXISTS(
          SELECT
            1
          FROM
            jsonb_array_elements(md.module_json -> 'metadata' -> 'po' -> 'optional') AS optional_po
          WHERE
            optional_po ->> 'po' LIKE po_id || '%'))
),
live_matching_modules AS(
  SELECT DISTINCT
    mpm.module
  FROM
    requested_pos
    JOIN modules.module_po_mandatory mpm ON mpm.specialization = po_id
      OR mpm.po = po_id
    UNION
    SELECT DISTINCT
      mpo.module
    FROM
      requested_pos
    JOIN modules.module_po_optional mpo ON mpo.specialization = po_id
      OR mpo.po = po_id
),
published_candidate_modules AS(
  SELECT
    module
  FROM
    draft_matching_modules
  UNION
  SELECT
    module
  FROM
    live_matching_modules
),
published_modules AS(
  SELECT DISTINCT ON(m.id)
    m,
    NULL::modules.created_module_in_draft AS cm,
    md,
    mpm_agg.po_mandatory AS po_mandatory,
    mpo_agg.po_optional AS po_optional
  FROM
    published_candidate_modules pcm
  JOIN modules.module m ON m.id = pcm.module
    LEFT JOIN modules.module_draft md ON md.module = m.id
    LEFT JOIN draft_matching_modules dmm ON dmm.module = m.id
    LEFT JOIN live_matching_modules lmm ON lmm.module = m.id
    LEFT JOIN LATERAL(
      SELECT
        array_agg(DISTINCT mpm.po) AS po_mandatory
      FROM
        modules.module_po_mandatory mpm
      WHERE
        mpm.module = m.id) mpm_agg ON TRUE
    LEFT JOIN LATERAL(
      SELECT
        array_agg(DISTINCT mpo.po) AS po_optional
      FROM
        modules.module_po_optional mpo
      WHERE
        mpo.module = m.id) mpo_agg ON TRUE
    WHERE
      NOT EXISTS(
        SELECT
          1
        FROM
          modules.created_module_in_draft cm
        WHERE
          cm.module = m.id)
        AND(md.module IS NOT NULL
          AND dmm.module IS NOT NULL
          OR md.module IS NULL
          AND lmm.module IS NOT NULL))
  SELECT
    CASE WHEN pos_param IS NULL
      OR array_length(pos_param, 1) IS NULL THEN
      '[]'::jsonb
    ELSE
      coalesce(json_agg(modules.build_module_info_json(FALSE, m, cm, md, po_mandatory, po_optional))::jsonb, '[]'::jsonb)
    END
  FROM(
    SELECT
      *
    FROM
      created_modules
    UNION
    SELECT
      *
    FROM
      published_modules) AS distinct_modules;
$$;

-- Answers whether a module belongs to any of the requested base POs across live,
-- draft, and preview-only sources. Base PO ids are matched against
-- specialization-aware values when needed.
CREATE OR REPLACE FUNCTION modules.module_of_po(module_param uuid, pos_param text[])
  RETURNS bool
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    CASE WHEN module_param IS NULL
      OR pos_param IS NULL
      OR array_length(pos_param, 1) IS NULL THEN
      FALSE
    ELSE
      EXISTS(
        SELECT
          1
        FROM
          unnest(pos_param) AS po_id
        WHERE
          po_id IS NOT NULL
          AND(
            -- Check mandatory PO relationships
            EXISTS(
              SELECT
                1
              FROM
                modules.module_po_mandatory
              WHERE
                module = module_param
                AND po = po_id)
              OR
              -- Check optional PO relationships
              EXISTS(
                SELECT
                  1
                FROM
                  modules.module_po_optional
                WHERE
                  module = module_param
                  AND po = po_id)
                OR
                -- Check module_draft
                EXISTS(
                  SELECT
                    1
                  FROM
                    modules.module_draft
                  WHERE
                    module = module_param
                    AND(EXISTS(
                        SELECT
                          1
                        FROM
                          jsonb_array_elements(module_json -> 'metadata' -> 'po' -> 'mandatory') AS elem
                        WHERE(elem ->> 'po')
                        LIKE po_id || '%')
                      OR EXISTS(
                        SELECT
                          1
                        FROM
                          jsonb_array_elements(module_json -> 'metadata' -> 'po' -> 'optional') AS elem
                        WHERE(elem ->> 'po')
                        LIKE po_id || '%')))
                  OR
                  -- Check created module in draft
                  EXISTS(
                    SELECT
                      1
                    FROM
                      modules.created_module_in_draft
                    WHERE
                      module = module_param
                      AND(EXISTS(
                          SELECT
                            1
                          FROM
                            unnest(module_mandatory_pos) AS full_po_id
                          WHERE
                            full_po_id LIKE po_id || '%')
                          OR EXISTS(
                            SELECT
                              1
                            FROM
                              unnest(module_optional_pos) AS full_po_id
                            WHERE
                              full_po_id LIKE po_id || '%')))))
    END;
$$;

-- Summarizes dashboard-level privilege and review counters for a user/campus
-- pair in one round trip.
CREATE OR REPLACE FUNCTION modules.get_user_info(uid text, cid text)
  RETURNS TABLE(
    has_director_privileges boolean,
    has_module_review_privileges boolean,
    has_modules_to_edit boolean,
    rejected_reviews integer,
    reviews_to_approve integer)
  STABLE
  LANGUAGE sql
  AS $$
  SELECT
    EXISTS(
      SELECT
        1
      FROM
        core.study_program_person p
      WHERE
        p.person = uid) AS has_director_privileges,
    EXISTS(
      SELECT
        1
      FROM
        core.study_program_person p
      WHERE
        p.person = uid
        AND p.role = 'pav') AS has_module_review_privileges,
    EXISTS(
      SELECT
        1
      FROM
        modules.module_update_permission m
      WHERE
        m.campus_id = cid) AS has_modules_to_edit,
(
      SELECT
        count(*)
      FROM
        modules.module_update_permission mp
        JOIN modules.module_review mr ON mp.module = mr.module_draft
      WHERE
        mp.campus_id = cid
        AND mr.status = 'rejected') AS rejected_reviews,
(
      SELECT
        count(DISTINCT mr.module_draft)
      FROM
        core.study_program_person sp
        JOIN modules.module_review mr ON sp.study_program = mr.study_program
          AND sp.role = mr.role
          AND mr.status = 'pending'
      WHERE
        sp.person = uid) AS reviews_to_approve;
$$;

-- Returns the identities that hold explicitly granted edit permissions for the
-- given module.
CREATE OR REPLACE FUNCTION modules.get_users_with_granted_permissions_from_module(module_id uuid)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(json_agg(i.id), '[]'::json)
  FROM
    modules.module_update_permission mup
    JOIN core.identity i ON mup.campus_id = i.campus_id
  WHERE
    mup.module = module_id
    AND mup.kind = 'granted'
$$;

-- Lists the published concrete module options that instantiate a given generic
-- module.
CREATE OR REPLACE FUNCTION modules.get_generic_module_options(module_id uuid)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(json_agg(modules.module_to_json_short(m) || jsonb_build_object('status', m.status)), '[]'::json)
  FROM( SELECT DISTINCT ON(m.id)
      m.*
    FROM
      modules.module_po_optional AS opt
      JOIN modules.module m ON m.id = opt.module
    WHERE
      instance_of = module_id) AS m;
$$;

-- Returns active generic modules for a PO, combining published rows with
-- preview-only created modules. Only mandatory PO assignments are considered.
CREATE OR REPLACE FUNCTION modules.generic_modules_for_po(po_id text)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(jsonb_agg(jsonb_build_object('id', m.id, 'title', m.title, 'abbrev', m.abbrev)), '[]'::jsonb)
  FROM( SELECT DISTINCT ON(m.id)
      m.id AS id,
      m.title AS title,
      m.abbrev AS abbrev
    FROM
      modules.module m
      JOIN modules.module_po_mandatory po ON po.module = m.id
        AND po.po = po_id
    WHERE
      m.module_type = 'generic_module'
      AND m.status = 'active'
    UNION( SELECT DISTINCT ON(cm.module)
        cm.module AS id,
        cm.module_title AS title,
        cm.module_abbrev AS abbrev
      FROM
        modules.created_module_in_draft cm
      WHERE
        cm.module_type = 'generic_module'
        AND po_id = ANY(module_mandatory_pos))) AS m;
$$;

-- Maps a month/year pair to the surrounding academic year window and returns
-- the semester plan entries that fall into that range.
CREATE OR REPLACE FUNCTION schedule.semester_plan_by_now(p_month integer, p_year integer)
  RETURNS jsonb
  LANGUAGE plpgsql
  STABLE
  AS $$
DECLARE
  v_start date;
  v_end date;
BEGIN
  IF p_month >= 3 AND p_month <= 8 THEN
    v_start := make_date(p_year, 3, 1);
    v_end := make_date(p_year + 1, 2, 28);
  ELSIF p_month >= 9 THEN
    v_start := make_date(p_year, 9, 1);
    v_end := make_date(p_year + 1, 8, 31);
  ELSE
    v_start := make_date(p_year - 1, 9, 1);
    v_end := make_date(p_year, 8, 31);
  END IF;
  RETURN (
    SELECT
      coalesce(jsonb_agg(row_to_json(t)::jsonb), '[]'::jsonb)
    FROM (
      SELECT
        sp.id,
        sp."start",
        sp."end",
        sp.type,
        tu.id AS "teachingUnit",
        tu.label AS "teachingUnitLabel",
        sp.semester_index AS "semesterIndex",
        sp.phase
      FROM
        schedule.semester_plan sp
      LEFT JOIN core.teaching_unit tu ON tu.id = sp.teaching_unit
    WHERE
      sp."start" BETWEEN v_start AND v_end) t);
END;
$$;

-- Exposes core module management metadata for callers that need the full module
-- set. Hot schedule entry functions below aggregate this only for the modules
-- present in each result set.
CREATE OR REPLACE VIEW modules.module_core_raw AS
SELECT
  m.id,
  m.title,
  m.abbrev,
  COALESCE(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
          i.lastname
        ELSE
          i.title
        END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
          i.abbreviation
        ELSE
          i.id
        END)) FILTER (WHERE i.id IS NOT NULL), '[]'::jsonb) AS module_management
FROM
  modules.module m
  LEFT JOIN modules.module_responsibility mr ON m.id = mr.module
    AND mr.responsibility_type = 'module_management'
  LEFT JOIN core.identity i ON mr.identity = i.id
GROUP BY
  m.id;

-- Returns enriched schedule entries for an explicit list of ids.
-- The payload joins rooms, lecturers, module metadata, and teaching units into
-- the JSON shape consumed by the schedule APIs.
-- NOTE: The two overloads below are intentionally duplicated to keep each one a
-- single-pass query. Any change to the SELECT/JOIN/aggregation body must be
-- applied to BOTH functions.
CREATE OR REPLACE FUNCTION schedule.get_schedule_entries(p_ids uuid[])
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH entries AS(
    SELECT
      se.id,
      se.series_id,
      se."start",
      se."end",
      se.course_type,
      se.module,
      se.rooms,
      se.lecturer,
      se.po
    FROM
      schedule.schedule_entry se
    WHERE
      se.id = ANY(p_ids)
),
module_core AS(
  SELECT
    m.id,
    m.title,
    m.abbrev,
    coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
            i.lastname
          ELSE
            i.title
          END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
            i.abbreviation
          ELSE
            i.id
          END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS module_management
  FROM( SELECT DISTINCT
      module
    FROM
      entries) em
    JOIN modules.module m ON m.id = em.module
    LEFT JOIN modules.module_responsibility mr ON m.id = mr.module
      AND mr.responsibility_type = 'module_management'
    LEFT JOIN core.identity i ON mr.identity = i.id
  GROUP BY
    m.id
)
SELECT
  coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'seriesId', s.series_id, 'start', s."start", 'end', s."end", 'courseType', s.course_type, 'rooms', rooms.room_agg, 'module', mc.id, 'moduleTitle', mc.title, 'moduleAbbrev', mc.abbrev, 'moduleManagement', mc.module_management, 'lecturer', lecturers.lecturer_agg, 'teachingUnits', mtu.teaching_units, 'po', s.po)), '[]'::jsonb)
FROM
  entries s
  JOIN module_core mc ON mc.id = s.module
  JOIN schedule.module_teaching_unit mtu ON mtu.module = mc.id
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'abbrev', r.abbrev)) FILTER(WHERE r.id IS NOT NULL), '[]'::jsonb) AS room_agg
    FROM
      unnest(s.rooms) AS room_id(id)
      LEFT JOIN schedule.room r ON r.id = room_id.id) rooms ON TRUE
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
              i.lastname
            ELSE
              i.title
            END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
              i.abbreviation
            ELSE
              i.id
            END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS lecturer_agg
    FROM
      unnest(s.lecturer) AS lec_id(id)
      LEFT JOIN core.identity i ON i.id = lec_id.id) lecturers ON TRUE
$$;

-- Returns enriched schedule entry drafts for an explicit list of ids.
-- The payload joins rooms, lecturers, module metadata, and teaching units into
-- the JSON shape consumed by the schedule APIs.
-- NOTE: The two overloads below are intentionally duplicated to keep each one a
-- single-pass query. Any change to the SELECT/JOIN/aggregation body must be
-- applied to BOTH functions.
CREATE OR REPLACE FUNCTION schedule.get_schedule_entry_drafts(p_ids uuid[])
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH entries AS(
    SELECT
      sed.id,
      sed.series_id,
      sed."start",
      sed."end",
      sed.course_type,
      sed.module,
      sed.rooms,
      sed.lecturer,
      sed.po
    FROM
      schedule.schedule_entry_draft sed
    WHERE
      sed.id = ANY(p_ids)
),
module_core AS(
  SELECT
    m.id,
    m.title,
    m.abbrev,
    coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
            i.lastname
          ELSE
            i.title
          END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
            i.abbreviation
          ELSE
            i.id
          END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS module_management
  FROM( SELECT DISTINCT
      module
    FROM
      entries) em
    JOIN modules.module m ON m.id = em.module
    LEFT JOIN modules.module_responsibility mr ON m.id = mr.module
      AND mr.responsibility_type = 'module_management'
    LEFT JOIN core.identity i ON mr.identity = i.id
  GROUP BY
    m.id
)
SELECT
  coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'seriesId', s.series_id, 'start', s."start", 'end', s."end", 'courseType', s.course_type, 'rooms', rooms.room_agg, 'module', mc.id, 'moduleTitle', mc.title, 'moduleAbbrev', mc.abbrev, 'moduleManagement', mc.module_management, 'lecturer', lecturers.lecturer_agg, 'teachingUnits', coalesce(mtu.teaching_units, ARRAY[]::uuid[]), 'po', s.po)), '[]'::jsonb)
FROM
  entries s
  JOIN module_core mc ON mc.id = s.module
  LEFT JOIN schedule.module_teaching_unit mtu ON mtu.module = mc.id
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'abbrev', r.abbrev)) FILTER(WHERE r.id IS NOT NULL), '[]'::jsonb) AS room_agg
    FROM
      unnest(s.rooms) AS room_id(id)
      LEFT JOIN schedule.room r ON r.id = room_id.id) rooms ON TRUE
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
              i.lastname
            ELSE
              i.title
            END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
              i.abbreviation
            ELSE
              i.id
            END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS lecturer_agg
    FROM
      unnest(s.lecturer) AS lec_id(id)
      LEFT JOIN core.identity i ON i.id = lec_id.id) lecturers ON TRUE
$$;

-- overload mentioned above
DROP FUNCTION IF EXISTS schedule.get_schedule_entry_drafts(uuid);

CREATE OR REPLACE FUNCTION schedule.get_schedule_entry_drafts(p_plan_draft uuid, p_start timestamp, p_end timestamp)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH entries AS(
    SELECT
      sed.id,
      sed.series_id,
      sed."start",
      sed."end",
      sed.course_type,
      sed.module,
      sed.rooms,
      sed.lecturer,
      sed.po
    FROM
      schedule.schedule_entry_draft sed
    WHERE
      sed.plan_draft = p_plan_draft
      AND sed."start" >= p_start
      AND sed."start" < p_end
      AND sed."end" <= p_end
),
module_core AS(
  SELECT
    m.id,
    m.title,
    m.abbrev,
    coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
            i.lastname
          ELSE
            i.title
          END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
            i.abbreviation
          ELSE
            i.id
          END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS module_management
  FROM( SELECT DISTINCT
      module
    FROM
      entries) em
    JOIN modules.module m ON m.id = em.module
    LEFT JOIN modules.module_responsibility mr ON m.id = mr.module
      AND mr.responsibility_type = 'module_management'
    LEFT JOIN core.identity i ON mr.identity = i.id
  GROUP BY
    m.id
)
SELECT
  coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'seriesId', s.series_id, 'start', s."start", 'end', s."end", 'courseType', s.course_type, 'rooms', rooms.room_agg, 'module', mc.id, 'moduleTitle', mc.title, 'moduleAbbrev', mc.abbrev, 'moduleManagement', mc.module_management, 'lecturer', lecturers.lecturer_agg, 'teachingUnits', coalesce(mtu.teaching_units, ARRAY[]::uuid[]), 'po', s.po)), '[]'::jsonb)
FROM
  entries s
  JOIN module_core mc ON mc.id = s.module
  LEFT JOIN schedule.module_teaching_unit mtu ON mtu.module = mc.id
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'abbrev', r.abbrev)) FILTER(WHERE r.id IS NOT NULL), '[]'::jsonb) AS room_agg
    FROM
      unnest(s.rooms) AS room_id(id)
      LEFT JOIN schedule.room r ON r.id = room_id.id) rooms ON TRUE
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
              i.lastname
            ELSE
              i.title
            END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
              i.abbreviation
            ELSE
              i.id
            END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS lecturer_agg
    FROM
      unnest(s.lecturer) AS lec_id(id)
      LEFT JOIN core.identity i ON i.id = lec_id.id) lecturers ON TRUE
$$;

-- overload mentioned above
CREATE OR REPLACE FUNCTION schedule.get_schedule_entries(p_start timestamp, p_end timestamp)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH entries AS(
    SELECT
      se.id,
      se.series_id,
      se."start",
      se."end",
      se.course_type,
      se.module,
      se.rooms,
      se.lecturer,
      se.po
    FROM
      schedule.schedule_entry se
    WHERE
      se."start" >= p_start
      AND se."start" < p_end
      AND se."end" <= p_end
),
module_core AS(
  SELECT
    m.id,
    m.title,
    m.abbrev,
    coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
            i.lastname
          ELSE
            i.title
          END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
            i.abbreviation
          ELSE
            i.id
          END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS module_management
  FROM( SELECT DISTINCT
      module
    FROM
      entries) em
    JOIN modules.module m ON m.id = em.module
    LEFT JOIN modules.module_responsibility mr ON m.id = mr.module
      AND mr.responsibility_type = 'module_management'
    LEFT JOIN core.identity i ON mr.identity = i.id
  GROUP BY
    m.id
)
SELECT
  coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'seriesId', s.series_id, 'start', s."start", 'end', s."end", 'courseType', s.course_type, 'rooms', rooms.room_agg, 'module', mc.id, 'moduleTitle', mc.title, 'moduleAbbrev', mc.abbrev, 'moduleManagement', mc.module_management, 'lecturer', lecturers.lecturer_agg, 'teachingUnits', mtu.teaching_units, 'po', s.po)), '[]'::jsonb)
FROM
  entries s
  JOIN module_core mc ON mc.id = s.module
  JOIN schedule.module_teaching_unit mtu ON mtu.module = mc.id
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'abbrev', r.abbrev)) FILTER(WHERE r.id IS NOT NULL), '[]'::jsonb) AS room_agg
    FROM
      unnest(s.rooms) AS room_id(id)
      LEFT JOIN schedule.room r ON r.id = room_id.id) rooms ON TRUE
  LEFT JOIN LATERAL(
    SELECT
      coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
              i.lastname
            ELSE
              i.title
            END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
              i.abbreviation
            ELSE
              i.id
            END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb) AS lecturer_agg
    FROM
      unnest(s.lecturer) AS lec_id(id)
      LEFT JOIN core.identity i ON i.id = lec_id.id) lecturers ON TRUE
$$;

-- Produces the compact module catalog payload used by module list endpoints.
-- When include_drafts is true, created-in-draft previews are merged with live
-- modules; otherwise only live modules are returned. The JSON shape is identical
-- in both cases.
CREATE OR REPLACE FUNCTION modules.module_core(include_drafts boolean)
  RETURNS jsonb
  LANGUAGE sql
  STABLE PARALLEL SAFE
  AS $$
  SELECT
    coalesce(jsonb_agg(module_json ORDER BY title), '[]'::jsonb)
  FROM( SELECT DISTINCT ON(id)
      id,
      title,
      module_json
    FROM(
      -- Live modules (priority 0)
      SELECT
        m.id,
        0 AS src_rank,
        m.title,
        jsonb_build_object('id', m.id, 'title', m.title, 'abbreviation', m.abbrev, 'moduleManagement', coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'lastname', CASE WHEN i.kind = 'person' THEN
                  i.lastname
                ELSE
                  i.title
                END, 'firstname', CASE WHEN i.kind = 'person' THEN
                  i.firstname
                ELSE
                  NULL
                END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb), 'ects', m.ects, 'isLive', TRUE) AS module_json
      FROM
        modules.module m
      LEFT JOIN modules.module_responsibility mr ON mr.module = m.id
        AND mr.responsibility_type = 'module_management'
    LEFT JOIN core.identity i ON i.id = mr.identity
  GROUP BY
    m.id
  UNION ALL
  -- Draft modules (priority 1, only when requested)
  SELECT
    cmd.module AS id,
    1 AS src_rank,
    cmd.module_title AS title,
    jsonb_build_object('id', cmd.module, 'title', cmd.module_title, 'abbreviation', cmd.module_abbrev, 'moduleManagement', coalesce(jsonb_agg(jsonb_build_object('id', i.id, 'kind', i.kind, 'lastname', CASE WHEN i.kind = 'person' THEN
              i.lastname
            ELSE
              i.title
            END, 'firstname', CASE WHEN i.kind = 'person' THEN
              i.firstname
            ELSE
              NULL
            END)) FILTER(WHERE i.id IS NOT NULL), '[]'::jsonb), 'ects', cmd.module_ects, 'isLive', FALSE) AS module_json
  FROM
    modules.created_module_in_draft cmd
  LEFT JOIN LATERAL unnest(cmd.module_management) AS mgmt_id ON TRUE
  LEFT JOIN core.identity i ON i.id = mgmt_id
WHERE
  include_drafts
GROUP BY
  cmd.module) combined
ORDER BY
  id,
  src_rank) sub;
$$;
