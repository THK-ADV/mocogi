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

DROP FUNCTION IF EXISTS exams.exam_list_options;

DROP FUNCTION IF EXISTS exams.exam_pool;

DROP FUNCTION IF EXISTS exams.exam_po_relations;

DROP FUNCTION IF EXISTS exams.remove_unrelated_exam_list_modules;

DROP FUNCTION IF EXISTS exams.module_ref(modules.module);

DROP FUNCTION IF EXISTS exams.module_ref(modules.module, core.identity);

DROP FUNCTION IF EXISTS schedule.identity_ref(core.identity);

DROP FUNCTION IF EXISTS schedule.set_exam_room_drafts;

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
        AND po.date_from <= now()
      AND(po.date_to IS NULL
        OR po.date_to >= now())
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
        AND po.date_from <= now()
      AND(po.date_to IS NULL
        OR po.date_to >= now())
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
    json_build_object('isModuleManager', inherited_perm, 'isNewModule', cm.module IS NOT NULL, 'module', json_build_object('id', coalesce(md.module, cm.module, m.id), 'title', coalesce(md.module_title, cm.module_title, m.title), 'abbreviation', coalesce(md.module_abbrev, cm.module_abbrev, m.abbrev)), 'ects', coalesce((md.module_json -> 'metadata' -> 'ects')::numeric, cm.module_ects, m.ects), 'status', coalesce(md.module_json -> 'metadata' ->> 'status', m.status), 'mandatoryPOs', po_mandatory, 'optionalPOs', po_optional, 'moduleDraftState', modules.calculate_module_draft_state(md), 'moduleDraft', CASE WHEN md.module IS NOT NULL THEN
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

-- Semester boundaries are supplied by Semester, including leap years.
CREATE OR REPLACE FUNCTION schedule.get_semester_plan(p_start date, p_end date)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(jsonb_agg(to_jsonb(t)), '[]'::jsonb)
  FROM(
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
    sp."start" >= p_start
    AND sp."start" < p_end) t
$$;

-- Booking enrichment stays in SQL. Keep both overloads in sync.
CREATE OR REPLACE FUNCTION schedule.get_bookings(p_ids uuid[])
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH entries AS(
    SELECT
      se.id,
      se.kind,
      se.title,
      se.note,
      se.created_by,
      se.updated_at,
      se.series_id,
      se."start",
      se."end",
      se.course_type,
      se.module,
      se.rooms,
      se.lecturer,
      se.po
    FROM
      schedule.booking se
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
),
rooms_by_entry AS(
  SELECT
    e.id,
    coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'abbrev', r.abbrev)) FILTER(WHERE r.id IS NOT NULL), '[]'::jsonb) AS room_agg
  FROM
    entries e
    CROSS JOIN LATERAL unnest(e.rooms) AS room_id(id)
    LEFT JOIN schedule.room r ON r.id = room_id.id
  GROUP BY
    e.id
),
lecturers_by_entry AS(
  SELECT
    e.id,
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
    entries e
    CROSS JOIN LATERAL unnest(e.lecturer) AS lecturer_id(id)
    LEFT JOIN core.identity i ON i.id = lecturer_id.id
  GROUP BY
    e.id
)
SELECT
  coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'seriesId', s.series_id, 'start', s."start", 'end', s."end", 'kind', s.kind, 'title', s.title, 'note', s.note, 'createdBy', jsonb_build_object('id', s.created_by, 'label', coalesce(nullif(
              CASE WHEN creator.kind = 'person' THEN
                concat_ws(' ', creator.firstname, creator.lastname)
              ELSE
                creator.title
              END, ''), s.created_by)), 'updatedAt', s.updated_at, 'rooms', rooms.room_agg, 'lecturer', lecturers.lecturer_agg) || CASE WHEN s.kind = 'teaching' THEN
        jsonb_build_object('courseType', s.course_type, 'module', mc.id, 'moduleTitle', mc.title, 'moduleAbbrev', mc.abbrev, 'moduleManagement', mc.module_management, 'teachingUnits', coalesce(to_jsonb(mtu.teaching_units), '[]'::jsonb), 'po', s.po)
      ELSE
        '{}'::jsonb
      END), '[]'::jsonb)
FROM
  entries s
  LEFT JOIN core.identity creator ON creator.id = s.created_by
  LEFT JOIN module_core mc ON mc.id = s.module
  LEFT JOIN schedule.module_teaching_unit mtu ON mtu.module = mc.id
  LEFT JOIN rooms_by_entry rooms ON rooms.id = s.id
  LEFT JOIN lecturers_by_entry lecturers ON lecturers.id = s.id
$$;

CREATE OR REPLACE FUNCTION schedule.get_bookings(p_kind text, p_start timestamptz, p_end timestamptz)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH entries AS(
    SELECT
      se.id,
      se.kind,
      se.title,
      se.note,
      se.created_by,
      se.updated_at,
      se.series_id,
      se."start",
      se."end",
      se.course_type,
      se.module,
      se.rooms,
      se.lecturer,
      se.po
    FROM
      schedule.booking se
    WHERE
      se.kind = p_kind
      AND se."start" >= p_start
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
),
rooms_by_entry AS(
  SELECT
    e.id,
    coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'abbrev', r.abbrev)) FILTER(WHERE r.id IS NOT NULL), '[]'::jsonb) AS room_agg
  FROM
    entries e
    CROSS JOIN LATERAL unnest(e.rooms) AS room_id(id)
    LEFT JOIN schedule.room r ON r.id = room_id.id
  GROUP BY
    e.id
),
lecturers_by_entry AS(
  SELECT
    e.id,
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
    entries e
    CROSS JOIN LATERAL unnest(e.lecturer) AS lecturer_id(id)
    LEFT JOIN core.identity i ON i.id = lecturer_id.id
  GROUP BY
    e.id
)
SELECT
  coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'seriesId', s.series_id, 'start', s."start", 'end', s."end", 'kind', s.kind, 'title', s.title, 'note', s.note, 'createdBy', jsonb_build_object('id', s.created_by, 'label', coalesce(nullif(
              CASE WHEN creator.kind = 'person' THEN
                concat_ws(' ', creator.firstname, creator.lastname)
              ELSE
                creator.title
              END, ''), s.created_by)), 'updatedAt', s.updated_at, 'rooms', rooms.room_agg, 'lecturer', lecturers.lecturer_agg) || CASE WHEN s.kind = 'teaching' THEN
        jsonb_build_object('courseType', s.course_type, 'module', mc.id, 'moduleTitle', mc.title, 'moduleAbbrev', mc.abbrev, 'moduleManagement', mc.module_management, 'teachingUnits', coalesce(to_jsonb(mtu.teaching_units), '[]'::jsonb), 'po', s.po)
      ELSE
        '{}'::jsonb
      END), '[]'::jsonb)
FROM
  entries s
  LEFT JOIN core.identity creator ON creator.id = s.created_by
  LEFT JOIN module_core mc ON mc.id = s.module
  LEFT JOIN schedule.module_teaching_unit mtu ON mtu.module = mc.id
  LEFT JOIN rooms_by_entry rooms ON rooms.id = s.id
  LEFT JOIN lecturers_by_entry lecturers ON lecturers.id = s.id
$$;

-- Returns the active exam list candidates of a PO sorted by title and id. Child modules
-- are never candidates; the PO relationships of active ones count for their parent module,
-- and their missing exam phases follow those of the parent in alphabetical order.
-- The semester is the smallest recommended semester of these relationships or
-- the maximum integer without any.
CREATE OR REPLACE FUNCTION exams.exam_list_options(po_id text)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH rel AS(
    SELECT
      module,
      specialization,
      recommended_semester,
      TRUE AS mandatory,
      NULL::uuid AS instance_of
    FROM
      modules.module_po_mandatory
    WHERE
      po = po_id
    UNION ALL
    SELECT
      module,
      specialization,
      recommended_semester,
      FALSE,
      instance_of
    FROM
      modules.module_po_optional
    WHERE
      po = po_id
),
candidate AS(
  SELECT
    coalesce(relation.parent, rel.module) AS id,
    jsonb_agg(DISTINCT jsonb_build_object('po', po_id, 'specialization', rel.specialization, 'recommendedSemester', rel.recommended_semester, 'mandatory', rel.mandatory, 'instanceOf', rel.instance_of)) AS po,
    bool_or(rel.mandatory) AS mandatory,
    -- a scalar subquery instead of a lateral unnest, whose row estimate inflates the following joins
    coalesce(min((
        SELECT
          min(semester)
        FROM unnest(rel.recommended_semester) AS semester)), 2147483647) AS semester,
    array_agg(relation.child) FILTER(WHERE relation.child IS NOT NULL) AS children
  FROM
    rel
    LEFT JOIN modules.module_relation relation ON relation.child = rel.module
    LEFT JOIN modules.module child ON child.id = relation.child
  WHERE
    relation.child IS NULL
    OR child.status = 'active'
  GROUP BY
    1
),
-- aggregated once for all candidates instead of once per candidate
method AS(
  SELECT
    assessment.module,
    jsonb_agg(jsonb_build_object('method', assessment.assessment_method, 'percentage', assessment.percentage)
    ORDER BY assessment.assessment_method) AS methods
  FROM
    modules.module_assessment_method assessment
    JOIN candidate ON candidate.id = assessment.module
  GROUP BY
    assessment.module
),
candidate_exam AS(
  SELECT
    assignment.module,
    jsonb_agg(jsonb_build_object('id', exam.id, 'name', exam.name)
    ORDER BY exam.name, exam.id) AS exams
  FROM
    candidate
    JOIN exams.exam_module assignment ON assignment.module = candidate.id
    JOIN exams.exam exam ON exam.id = assignment.exam
  GROUP BY
    assignment.module
)
SELECT
  coalesce(jsonb_agg(jsonb_build_object('id', module.id, 'title', module.title, 'abbrev', module.abbrev, 'isGeneric', module.module_type = 'generic_module', 'mandatory', candidate.mandatory, 'semester', candidate.semester, 'po', candidate.po, 'assessmentMethods', coalesce(method.methods, '[]'), 'firstExaminer', module.first_examiner, 'secondExaminer', module.second_examiner,
        -- CASE skips the lookup for the majority of candidates without children
        'examPhases', CASE WHEN candidate.children IS NULL THEN
          module.exam_phases
        ELSE
          module.exam_phases || ARRAY( SELECT DISTINCT
              phase
            FROM modules.module child
          CROSS JOIN unnest(child.exam_phases) AS phase
        WHERE
          child.id = ANY(candidate.children)
          AND phase <> ALL(module.exam_phases)
        ORDER BY phase)
        END, 'exams', CASE WHEN module.module_type = 'generic_module' THEN
          '[]'
        ELSE
          coalesce(candidate_exam.exams, '[]')
        END)
    ORDER BY module.title, module.id), '[]')
FROM
  candidate
  JOIN modules.module module ON module.id = candidate.id
  LEFT JOIN method ON method.module = module.id
  LEFT JOIN candidate_exam ON candidate_exam.module = module.id
WHERE
  module.status = 'active'
$$;

-- Removes the given modules from the exam list configurations of POs that
-- neither the modules nor their child modules relate to anymore, e.g. after a
-- module sync or deletion. Only configurations referencing such a module are
-- written. Returns their number.
CREATE OR REPLACE FUNCTION exams.remove_unrelated_exam_list_modules(modules uuid[])
  RETURNS integer
  LANGUAGE sql
  AS $$
  -- the given modules referenced by each configuration
  WITH referenced AS(
    SELECT
      list_config.po,
      ref.module
    FROM
      exams.exam_list_config list_config
    CROSS JOIN LATERAL(
      SELECT
        jsonb_array_elements_text(list_config.config -> 'excludedModuleIds')
    UNION
    SELECT
      jsonb_path_query(list_config.config, '$.excludedExams[*].module') #>> '{}'
    UNION
    SELECT
      jsonb_path_query(list_config.config, '$.registrationOverrides[*].module') #>> '{}') AS ref(module)
  WHERE
    ref.module::uuid = ANY(modules)
),
-- the referenced modules still related to a PO, child relations counting for the parent
related AS(
  SELECT
    po_relation.po,
    coalesce(relation.parent, po_relation.module) AS module
  FROM(
    SELECT
      po,
      module
    FROM
      modules.module_po_mandatory
    UNION ALL
    SELECT
      po,
      module
    FROM
      modules.module_po_optional) po_relation
    LEFT JOIN modules.module_relation relation ON relation.child = po_relation.module
  WHERE
    coalesce(relation.parent, po_relation.module) IN(
      SELECT
        referenced.module::uuid
      FROM
        referenced)
),
removed AS(
  SELECT
    referenced.po,
    jsonb_build_object('ids', jsonb_agg(referenced.module)) AS vars
  FROM
    referenced
  WHERE
    NOT EXISTS(
      SELECT
      FROM
        related
      WHERE
        related.po = referenced.po
        AND related.module = referenced.module::uuid)
    GROUP BY
      referenced.po
),
-- keeps the entries whose module is not among the removed ids
updated AS(
  UPDATE
    exams.exam_list_config list_config
  SET
    -- paths on the whole config yield [] instead of null for a missing array
    config = list_config.config || jsonb_build_object('excludedModuleIds', jsonb_path_query_array(list_config.config, '$.excludedModuleIds[*] ? (!(@ == $ids[*]))', removed.vars), 'excludedExams', jsonb_path_query_array(list_config.config, '$.excludedExams[*] ? (!(@.module == $ids[*]))', removed.vars), 'registrationOverrides', jsonb_path_query_array(list_config.config, '$.registrationOverrides[*] ? (!(@.module == $ids[*]))', removed.vars))
  FROM
    removed
  WHERE
    removed.po = list_config.po
  RETURNING
    1
)
SELECT
  count(*)::integer
FROM
  updated
$$;

-- Replaces the entries of the exam list by p_entries, the stored columns of
-- each entry as camelCase JSON. Only rows that change are written, so the
-- entries of retained modules keep their ids.
CREATE OR REPLACE FUNCTION exams.replace_exam_list_entries(p_exam_list uuid, p_entries jsonb)
  RETURNS void
  LANGUAGE sql
  AS $$
  DELETE FROM exams.exam_list_entry entry
  WHERE entry.exam_list = p_exam_list
    AND NOT EXISTS(
      SELECT
      FROM
        jsonb_array_elements(p_entries) AS published(value)
      WHERE(published.value ->> 'module')::uuid = entry.module);
INSERT INTO exams.exam_list_entry AS entry(id, exam_list, module, module_title, module_abbrev, mandatory, po, assessment_methods, first_examiner, second_examiner, exam_phases, exams)
SELECT
  gen_random_uuid(),
  p_exam_list,
  published.module,
  published."moduleTitle",
  published."moduleAbbrev",
  published.mandatory,
  published.po,
  published."assessmentMethods",
  published."firstExaminer",
  published."secondExaminer",
  published."examPhases",
  published.exams
FROM
  jsonb_to_recordset(p_entries) AS published(module uuid,
    "moduleTitle" text,
    "moduleAbbrev" text,
    mandatory boolean,
    po jsonb,
    "assessmentMethods" text[],
    "firstExaminer" text,
    "secondExaminer" text,
    "examPhases" text[],
    exams jsonb)
  -- unchanged rows are skipped, as ON CONFLICT would lock them
WHERE
  NOT EXISTS(
    SELECT
    FROM
      exams.exam_list_entry CURRENT
    WHERE
      current.exam_list = p_exam_list
      AND current.module = published.module
      AND(current.module_title,
        current.module_abbrev,
        current.mandatory,
        current.po,
        current.assessment_methods,
        current.first_examiner,
        current.second_examiner,
        current.exam_phases,
        current.exams) IS NOT DISTINCT FROM(published."moduleTitle",
        published."moduleAbbrev",
        published.mandatory,
        published.po,
        published."assessmentMethods",
        published."firstExaminer",
        published."secondExaminer",
        published."examPhases",
        published.exams))
ON CONFLICT(exam_list,
  module)
  DO UPDATE SET
    module_title = excluded.module_title,
    module_abbrev = excluded.module_abbrev,
    mandatory = excluded.mandatory,
    po = excluded.po,
    assessment_methods = excluded.assessment_methods,
    first_examiner = excluded.first_examiner,
    second_examiner = excluded.second_examiner,
    exam_phases = excluded.exam_phases,
    exams = excluded.exams;
$$;

-- Serializes an identity into the compact reference used by schedule and exam
-- payloads. Like the other builders of these payloads it returns json, which
-- is nested by copying text, and the caller casts the whole payload to jsonb
-- once instead of re-encoding every nested jsonb value at each level.
CREATE OR REPLACE FUNCTION schedule.identity_ref(i core.identity)
  RETURNS json
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    json_build_object('id', i.id, 'kind', i.kind, 'label', CASE WHEN i.kind = 'person' THEN
        i.lastname
      ELSE
        i.title
      END, 'abbreviation', CASE WHEN i.kind = 'person' THEN
        i.abbreviation
      ELSE
        i.id
      END);
$$;

-- Serializes a module with its first examiner for the exam management. The
-- caller joins the examiner, so that the function is inlined instead of looking
-- it up per module.
CREATE OR REPLACE FUNCTION exams.module_ref(m modules.module, examiner core.identity)
  RETURNS json
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    json_build_object('id', m.id, 'title', m.title, 'abbrev', m.abbrev, 'ects', m.ects, 'status', m.status, 'firstExaminer', schedule.identity_ref(examiner));
$$;

-- Returns the exams with their modules sorted by name and id, all if p_ids is
-- NULL. Modules are sorted by title and id.
CREATE OR REPLACE FUNCTION exams.get_exams(p_ids uuid[])
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH selected AS(
    SELECT
      *
    FROM
      exams.exam exam
    WHERE
      p_ids IS NULL
      OR exam.id = ANY(p_ids)
),
exam_modules AS(
  SELECT
    assignment.exam,
    json_agg(exams.module_ref(assigned_module, examiner)
    ORDER BY assigned_module.title, assigned_module.id) AS modules
  FROM
    selected
    JOIN exams.exam_module assignment ON assignment.exam = selected.id
    JOIN modules.module assigned_module ON assigned_module.id = assignment.module
    JOIN core.identity examiner ON examiner.id = assigned_module.first_examiner
  GROUP BY
    assignment.exam
),
exam_methods AS(
  SELECT
    assignment.exam,
    array_agg(DISTINCT method.assessment_method ORDER BY method.assessment_method) AS methods
  FROM
    selected
    JOIN exams.exam_module assignment ON assignment.exam = selected.id
    JOIN modules.module_assessment_method method ON method.module = assignment.module
  GROUP BY
    assignment.exam
)
SELECT
  coalesce(json_agg(json_build_object('id', selected.id, 'name', selected.name, 'assessmentMethods', coalesce(exam_methods.methods, '{}'), 'modules', coalesce(exam_modules.modules, '[]'))
    ORDER BY selected.name, selected.id), '[]')::jsonb
FROM
  selected
  LEFT JOIN exam_modules ON exam_modules.exam = selected.id
  LEFT JOIN exam_methods ON exam_methods.exam = selected.id
$$;

-- Returns the active modules with own exams (neither generic nor child
-- modules) without any exam, sorted by title and id.
CREATE OR REPLACE FUNCTION exams.modules_without_exam()
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  SELECT
    coalesce(json_agg(exams.module_ref(module, examiner)
      ORDER BY module.title, module.id), '[]')::jsonb
  FROM
    modules.module module
    JOIN core.identity examiner ON examiner.id = module.first_examiner
  WHERE
    module.status = 'active'
    AND module.module_type <> 'generic_module'
    AND NOT EXISTS(
      SELECT
      FROM
        modules.module_relation relation
      WHERE
        relation.child = module.id)
      AND NOT EXISTS(
        SELECT
        FROM
          exams.exam_module assignment
        WHERE
          assignment.module = module.id)
$$;

-- Returns the title of a live module and its exams with all their modules,
-- sorted by name and id resp. title and id, or NULL if the module does not
-- exist. Child and generic modules have no own exams and thus no exams.
CREATE OR REPLACE FUNCTION exams.module_exams(p_module uuid)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH OWNER AS(
    SELECT
      module.title,
      module.module_type <> 'generic_module'
      AND NOT EXISTS(
        SELECT
        FROM
          modules.module_relation relation
        WHERE
          relation.child = module.id) AS has_own_exams
    FROM
      modules.module module
    WHERE
      module.id = p_module
)
SELECT
  jsonb_build_object('title', owner.title, 'hasOwnExams', owner.has_own_exams, 'exams', CASE WHEN owner.has_own_exams THEN
      coalesce((
        SELECT
          jsonb_agg(jsonb_build_object('id', exam.id, 'name', exam.name, 'modules',(
                SELECT
                  jsonb_agg(jsonb_build_object('id', module.id, 'title', module.title, 'abbrev', module.abbrev)
                ORDER BY module.title, module.id)
              FROM exams.exam_module assignment
              JOIN modules.module module ON module.id = assignment.module
            WHERE
              assignment.exam = exam.id))
        ORDER BY exam.name, exam.id)
        FROM exams.exam_module own
        JOIN exams.exam exam ON exam.id = own.exam
      WHERE
        own.module = p_module), '[]')
    ELSE
      '[]'
    END)
FROM
  OWNER
$$;

-- Reconciles the exam assignments of the given live modules after a module
-- sync or deletion changed their types or relations. p_owning are those that
-- had own exams before. Modules without own exams lose their assignments,
-- modules that gained own exams get an exam named after them unless they are
-- assigned already, and the others keep their assignments, even none.
CREATE OR REPLACE FUNCTION exams.reconcile_module_exams(p_modules uuid[], p_owning uuid[])
  RETURNS void
  LANGUAGE sql
  AS $$
  DELETE FROM exams.exam_module assignment USING modules.module module
WHERE module.id = assignment.module
  AND module.id = ANY(p_modules)
  AND(module.module_type = 'generic_module'
    OR EXISTS(
      SELECT
      FROM
        modules.module_relation relation
      WHERE
        relation.child = module.id));
  WITH gained AS(
    SELECT
      gen_random_uuid() AS exam,
      module.id AS module,
      module.title
    FROM
      modules.module module
    WHERE
      module.id = ANY(p_modules)
      AND module.id <> ALL(p_owning)
      AND module.module_type <> 'generic_module'
      AND NOT EXISTS(
        SELECT
        FROM
          modules.module_relation relation
        WHERE
          relation.child = module.id)
        AND NOT EXISTS(
          SELECT
          FROM
            exams.exam_module assignment
          WHERE
            assignment.module = module.id)
),
created AS(
INSERT INTO exams.exam(id, name)
  SELECT
    exam,
    title
  FROM
    gained)
INSERT INTO exams.exam_module(exam, module)
SELECT
  exam,
  module
FROM
  gained;
$$;

-- Replaces the source exam ids in a jsonb array of objects with an `exam` key
-- by the target and removes the objects that then equal an earlier one in all
-- p_keys, keeping the order.
CREATE OR REPLACE FUNCTION exams.repoint_exam_refs(p_items jsonb, p_keys text[], p_target uuid, p_sources uuid[])
  RETURNS jsonb
  LANGUAGE sql
  IMMUTABLE
  AS $$
  SELECT
    coalesce(jsonb_agg(deduplicated.item ORDER BY deduplicated.position), '[]')
  FROM( SELECT DISTINCT ON(key)
      repointed.item,
      original.position
    FROM
      jsonb_array_elements(p_items)
      WITH ORDINALITY AS original(item, position)
      CROSS JOIN LATERAL(
        SELECT
          CASE WHEN(original.item ->> 'exam')::uuid = ANY(p_sources) THEN
            original.item || jsonb_build_object('exam', p_target)
          ELSE
            original.item
          END) AS repointed(item)
        CROSS JOIN LATERAL(
          SELECT
            ARRAY(
              SELECT
                repointed.item ->> key_name.name
              FROM
                unnest(p_keys)
                WITH ORDINALITY AS key_name(name, position)
              ORDER BY
                key_name.position)) AS item_key(key)
              ORDER BY
                key, original.position) AS deduplicated
$$;

-- Merges the source exams into the target exam, which gets p_name and the
-- modules of all of them. Every reference to a source then points to the
-- target: module assignments, published exam list entries, exam list
-- configurations and exam appointments. Duplicates created by re-pointing are
-- removed, the first occurrence stays. Only rows that change are written. The
-- sources are deleted. The caller locks the exams and validates the ids.
CREATE OR REPLACE FUNCTION exams.merge_exams(p_target uuid, p_sources uuid[], p_name text)
  RETURNS void
  LANGUAGE sql
  AS $$
  UPDATE
    exams.exam
  SET
    name = p_name
  WHERE
    id = p_target
    AND name <> p_name;
  INSERT INTO exams.exam_module(exam, module)
  SELECT DISTINCT
    p_target,
    source_assignment.module
  FROM
    exams.exam_module source_assignment
  WHERE
    source_assignment.exam = ANY(p_sources)
  ON CONFLICT
    DO NOTHING;
    -- existential ==: true if any exam of the entry equals any source. The
    -- subquery builds the variables once instead of per entry.
    UPDATE
      exams.exam_list_entry entry
    SET
      exams = exams.repoint_exam_refs(entry.exams, ARRAY['exam'], p_target, p_sources)
    WHERE
      jsonb_path_exists(entry.exams, '$[*].exam ? (@ == $sources[*])',(
          SELECT
            jsonb_build_object('sources', p_sources)));
      UPDATE
        exams.exam_list_config list_config
      SET
        config = list_config.config || jsonb_build_object('excludedExams', exams.repoint_exam_refs(list_config.config -> 'excludedExams', ARRAY['module', 'exam'], p_target, p_sources), 'registrationOverrides', exams.repoint_exam_refs(list_config.config -> 'registrationOverrides', ARRAY['module', 'exam', 'phase'], p_target, p_sources))
      WHERE
        jsonb_path_exists(list_config.config, '$ ? (@.excludedExams[*].exam == $sources[*] || @.registrationOverrides[*].exam == $sources[*])', jsonb_build_object('sources', p_sources));
        UPDATE
          schedule.exam_entry_draft draft
        SET
          exam = p_target
        WHERE
          draft.exam = ANY(p_sources);
          UPDATE
            schedule.exam_entry live
          SET
            exam = p_target
          WHERE
            live.exam = ANY(p_sources);
            DELETE FROM exams.exam
            WHERE id = ANY(p_sources);
$$;

-- Returns per exam its active modules with at least one relation to a PO valid
-- today, own or of an active child module, and these relations without the
-- generic module of elective relations. Like modules.resolve_po_relationships
-- for the module details, other POs are left out. Exams without any are missing.
CREATE OR REPLACE FUNCTION exams.exam_po_relations(p_exams uuid[])
  RETURNS TABLE(
    exam uuid,
    modules uuid[],
    po jsonb)
  LANGUAGE sql
  STABLE
  AS $$
  -- a join instead of = ANY, which compares each assignment with every element of p_exams
  WITH selected_assignment AS(
    SELECT
      assignment.exam,
      assignment.module
    FROM
      unnest(p_exams) AS selected(exam)
      JOIN exams.exam_module assignment ON assignment.exam = selected.exam
      JOIN modules.module assigned ON assigned.id = assignment.module
    WHERE
      assigned.status = 'active'
),
-- an assigned module and its active children, whose relations count for it
related AS(
  SELECT
    selected_assignment.exam,
    selected_assignment.module AS assigned,
    selected_assignment.module
  FROM
    selected_assignment
  UNION
  SELECT
    selected_assignment.exam,
    selected_assignment.module,
    relation.child
  FROM
    selected_assignment
    JOIN modules.module_relation relation ON relation.parent = selected_assignment.module
    JOIN modules.module child ON child.id = relation.child
  WHERE
    child.status = 'active'
),
po_relation AS(
  SELECT
    module,
    po,
    specialization,
    recommended_semester,
    TRUE AS mandatory
  FROM
    modules.module_po_mandatory
  UNION ALL
  SELECT
    module,
    po,
    specialization,
    recommended_semester,
    FALSE
  FROM
    modules.module_po_optional
)
SELECT
  related.exam,
  array_agg(DISTINCT related.assigned),
  jsonb_agg(DISTINCT jsonb_build_object('po', po_relation.po, 'specialization', po_relation.specialization, 'recommendedSemester', po_relation.recommended_semester, 'mandatory', po_relation.mandatory))
FROM
  related
  JOIN po_relation ON po_relation.module = related.module
  JOIN core.po valid_po ON valid_po.id = po_relation.po
WHERE
  valid_po.date_from <= now()
  AND(valid_po.date_to IS NULL
    OR valid_po.date_to >= now())
GROUP BY
  related.exam
$$;

-- Returns the planning pool of a semester, sorted by exam name, exam and
-- phase. An offer is an exam and a registration phase of the published exam
-- lists of the semester. Its `sources` are the list rows with the exam in that
-- phase, the most recent list first (list and row ids as tiebreakers), and its first
-- examiners and methods their distinct unions. Selected are the offers in
-- one of p_phases where any source has one of p_methods, an empty array
-- matching any, and that have no appointment in the semester's exam plan yet.
-- Names and modules are live.
CREATE OR REPLACE FUNCTION exams.exam_pool(p_semester text, p_phases text[], p_methods text[])
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH source AS(
    SELECT
(list_exam.value ->> 'exam')::uuid AS exam,
      period.key AS phase,
      period.value AS registration_period,
      list.id AS exam_list,
      list.po AS list_po,
      list.date,
      entry.id AS entry,
      entry.module,
      entry.module_title,
      entry.module_abbrev,
      entry.first_examiner,
      entry.second_examiner,
      entry.assessment_methods
    FROM
      exams.exam_list LIST
      JOIN exams.exam_list_entry entry ON entry.exam_list = list.id
      CROSS JOIN jsonb_array_elements(entry.exams) AS list_exam(value)
      CROSS JOIN jsonb_each(list_exam.value -> 'registrationPeriods') AS period(key,
        value)
    WHERE
      list.semester = p_semester
),
-- the phase is part of the offer, so filtering sources by phase keeps whole offers
listed AS(
  SELECT
    source.exam,
    source.phase,
    json_agg(json_build_object('examListEntry', source.entry, 'examList', source.exam_list, 'po', source.list_po, 'date', source.date, 'module', source.module, 'moduleTitle', source.module_title, 'moduleAbbrev', source.module_abbrev, 'firstExaminer', source.first_examiner, 'secondExaminer', source.second_examiner, 'assessmentMethods', source.assessment_methods, 'registrationPeriod', source.registration_period)
    ORDER BY source.date DESC, source.exam_list, source.entry) AS sources,
    array_agg(DISTINCT source.first_examiner) AS first_examiners,
    -- arrays of different lengths cannot be aggregated into an array, so they are flattened below
    jsonb_agg(source.assessment_methods) AS method_lists
  FROM
    source
  WHERE(cardinality(p_phases) = 0
    OR source.phase = ANY(p_phases))
  -- planned offers leave the pool
  AND NOT EXISTS(
    SELECT
    FROM
      schedule.plan_draft plan
      JOIN schedule.exam_entry_draft draft ON draft.plan_draft = plan.id
    WHERE
      plan.kind = 'exam'
      AND plan.semester = p_semester
      AND draft.exam = source.exam
      AND draft.phase = source.phase)
  GROUP BY
    source.exam,
    source.phase
  HAVING
    cardinality(p_methods) = 0
    OR bool_or(source.assessment_methods && p_methods)
),
offer AS(
  SELECT
    listed.exam,
    listed.phase,
    listed.sources,
    listed.first_examiners,
    ARRAY( SELECT DISTINCT
        method
      FROM
        jsonb_array_elements(listed.method_lists) AS method_list(value)
        CROSS JOIN jsonb_array_elements_text(method_list.value) AS method
      ORDER BY
        method) AS assessment_methods
    FROM
      listed
),
-- the first examiners of all offers, serialized once instead of looked up per offer
offer_examiner AS(
  SELECT
    offer.exam,
    offer.phase,
    json_agg(schedule.identity_ref(examiner)
    ORDER BY examiner.id) AS first_examiners
  FROM
    offer
    CROSS JOIN unnest(offer.first_examiners) AS first_examiner(id)
    JOIN core.identity examiner ON examiner.id = first_examiner.id
  GROUP BY
    offer.exam,
    offer.phase
),
live_module AS(
  SELECT
    assignment.exam,
    json_agg(json_build_object('id', module.id, 'title', module.title, 'abbrev', module.abbrev)
    ORDER BY module.title, module.id) AS modules
  FROM
    exams.exam_module assignment
    JOIN modules.module module ON module.id = assignment.module
  WHERE
    assignment.exam IN(
      SELECT
        offer.exam
      FROM
        offer)
    GROUP BY
      assignment.exam
)
SELECT
  coalesce(json_agg(json_build_object('exam', offer.exam, 'examName', exam.name, 'phase', offer.phase, 'modules', coalesce(live_module.modules, '[]'), 'firstExaminers', coalesce(offer_examiner.first_examiners, '[]'), 'assessmentMethods', offer.assessment_methods, 'sources', offer.sources)
    ORDER BY exam.name, offer.exam, offer.phase), '[]')::jsonb
FROM
  offer
  -- published rows may reference exams deleted since
  JOIN exams.exam exam ON exam.id = offer.exam
  LEFT JOIN offer_examiner ON offer_examiner.exam = offer.exam
    AND offer_examiner.phase = offer.phase
  LEFT JOIN live_module ON live_module.exam = offer.exam
$$;

-- Replaces the room allocations of an exam appointment draft by p_rooms
-- [{ room, allocatedSeats, requiredInvigilators, invigilators, partitionBy, partitionValue }], writing only
-- the rows that change, and returns whether any did. Both writes see the same snapshot, which is safe as
-- they affect disjoint rooms.
CREATE OR REPLACE FUNCTION schedule.set_exam_room_drafts(p_exam_entry_draft uuid, p_rooms jsonb)
  RETURNS boolean
  LANGUAGE sql
  AS $$
  WITH removed AS(
    DELETE FROM schedule.exam_room_draft draft_room
    WHERE draft_room.exam_entry_draft = p_exam_entry_draft
      AND NOT EXISTS(
        SELECT
        FROM
          jsonb_array_elements(p_rooms) AS allocation(value)
        WHERE(allocation.value ->> 'room')::uuid = draft_room.room)
    RETURNING
      1
),
written AS(
INSERT INTO schedule.exam_room_draft AS draft_room(exam_entry_draft, room, allocated_seats, required_invigilators, invigilators, partition_by, partition_value)
  SELECT
    p_exam_entry_draft,
    allocation.room,
    allocation."allocatedSeats",
    allocation."requiredInvigilators",
    allocation.invigilators,
    allocation."partitionBy",
    allocation."partitionValue"
  FROM
    jsonb_to_recordset(p_rooms) AS allocation(room uuid,
      "allocatedSeats" integer,
      "requiredInvigilators" integer,
      invigilators text[],
      "partitionBy" text,
      "partitionValue" text)
  ON CONFLICT(exam_entry_draft,
    room)
    DO UPDATE SET
      allocated_seats = excluded.allocated_seats, required_invigilators = excluded.required_invigilators, invigilators = excluded.invigilators, partition_by = excluded.partition_by, partition_value = excluded.partition_value
    WHERE(draft_room.allocated_seats, draft_room.required_invigilators, draft_room.invigilators, draft_room.partition_by, draft_room.partition_value) IS DISTINCT FROM(excluded.allocated_seats, excluded.required_invigilators, excluded.invigilators, excluded.partition_by, excluded.partition_value)
  RETURNING
    1)
  SELECT
    EXISTS(
      SELECT
      FROM
        removed)
    OR EXISTS(
      SELECT
      FROM
        written)
$$;

-- Returns enriched exam appointments sorted by start and id: the drafts of
-- p_plan_draft if given, otherwise the published ones of p_semester (all if
-- NULL). p_ids restricts them to the ids and p_from/p_to to those overlapping
-- the interval, NULL skips the filter. The exam name, its active modules of
-- POs valid today with ECTS and first examiners, their PO relations, rooms and
-- people are resolved
-- once for all selected appointments. Each side gets only what it uses: drafts
-- the exam, phase, module methods, room capacities and the planning figures,
-- which are not published; published appointments the teaching units of their
-- modules for filtering.
CREATE OR REPLACE FUNCTION schedule.get_exam_entries(p_plan_draft uuid, p_semester text, p_ids uuid[], p_from timestamptz, p_to timestamptz)
  RETURNS jsonb
  LANGUAGE sql
  STABLE
  AS $$
  WITH entry AS(
    SELECT
      draft.id,
      draft.exam,
      draft.phase,
      draft.assessment_method,
      draft."start",
      draft."end",
      draft.expected_participants
    FROM
      schedule.exam_entry_draft draft
    WHERE
      draft.plan_draft = p_plan_draft
    UNION ALL
    SELECT
      live.id,
      live.exam,
      live.phase,
      live.assessment_method,
      live."start",
      live."end",
      NULL
    FROM
      schedule.exam_entry live
    WHERE
      p_plan_draft IS NULL
      AND(p_semester IS NULL
        OR live.semester = p_semester)
),
selected AS(
  SELECT
    *
  FROM
    entry
    -- CASE instead of OR keeps the planner from estimating the optional filters as almost no rows
  WHERE
    CASE WHEN p_ids IS NULL THEN
      TRUE
    ELSE
      entry.id = ANY(p_ids)
  END
  AND CASE WHEN p_from IS NULL THEN
    TRUE
  ELSE
    entry."start" < p_to
    AND entry."end" > p_from
  END
),
selected_exam AS(
  SELECT DISTINCT
    selected.exam
  FROM
    selected
),
exam_po AS(
  SELECT
    *
  FROM
    exams.exam_po_relations(ARRAY(
        SELECT
          selected_exam.exam
        FROM selected_exam))
),
-- only active modules of POs valid today, see exams.exam_po_relations
exam_module AS(
  SELECT
    exam_po.exam,
    module.id,
    module.title,
    module.abbrev,
    module.ects,
    module.first_examiner
  FROM
    exam_po
    CROSS JOIN unnest(exam_po.modules) AS valid_module(id)
    JOIN modules.module module ON module.id = valid_module.id
),
-- assessment methods of the modules, which only planning needs
method AS(
  SELECT
    assessment.module,
    array_agg(assessment.assessment_method ORDER BY assessment.assessment_method) AS methods
  FROM
    modules.module_assessment_method assessment
  WHERE
    p_plan_draft IS NOT NULL
    AND assessment.module IN(
      SELECT
        exam_module.id
      FROM
        exam_module)
    GROUP BY
      assessment.module
),
-- rooms by index per selected appointment
allocation AS(
  SELECT
    selected.id AS exam_entry,
    allocated.room,
    allocated.allocated_seats,
    allocated.required_invigilators,
    allocated.invigilators,
    allocated.partition_by,
    allocated.partition_value
  FROM
    selected
    CROSS JOIN LATERAL(
      SELECT
        draft_room.room,
        draft_room.allocated_seats,
        draft_room.required_invigilators,
        draft_room.invigilators,
        draft_room.partition_by,
        draft_room.partition_value
      FROM
        schedule.exam_room_draft draft_room
      WHERE
        p_plan_draft IS NOT NULL
        AND draft_room.exam_entry_draft = selected.id
      UNION ALL
      SELECT
        live_room.room,
        NULL,
        NULL,
        live_room.invigilators,
        live_room.partition_by,
        live_room.partition_value
      FROM
        schedule.exam_room live_room
      WHERE
        p_plan_draft IS NULL
        AND live_room.exam_entry = selected.id) AS allocated
),
first_examiner AS(
  SELECT
    identity.id,
    schedule.identity_ref(IDENTITY) AS ref
  FROM
    core.identity IDENTITY
  WHERE
    identity.id IN(
      SELECT
        exam_module.first_examiner
      FROM
        exam_module)
),
exam_modules AS(
  SELECT
    exam_module.exam,
    json_agg(
      CASE WHEN p_plan_draft IS NULL THEN
        json_build_object('id', exam_module.id, 'title', exam_module.title, 'abbrev', exam_module.abbrev, 'ects', exam_module.ects, 'firstExaminer', first_examiner.ref)
      ELSE
        json_build_object('id', exam_module.id, 'title', exam_module.title, 'abbrev', exam_module.abbrev, 'ects', exam_module.ects, 'firstExaminer', first_examiner.ref, 'assessmentMethods', coalesce(method.methods, '{}'))
      END ORDER BY exam_module.title, exam_module.id) AS modules
  FROM
    exam_module
    LEFT JOIN first_examiner ON first_examiner.id = exam_module.first_examiner
    LEFT JOIN method ON method.module = exam_module.id
  GROUP BY
    exam_module.exam
),
-- teaching units of the modules, which only filtering published appointments needs
exam_teaching_units AS(
  SELECT
    exam_module.exam,
    array_agg(DISTINCT teaching_unit) AS teaching_units
  FROM
    exam_module
    JOIN schedule.module_teaching_unit module_unit ON module_unit.module = exam_module.id
    CROSS JOIN unnest(module_unit.teaching_units) AS teaching_unit
  WHERE
    p_plan_draft IS NULL
  GROUP BY
    exam_module.exam
),
allocation_invigilators AS(
  SELECT
    allocation.exam_entry,
    allocation.room,
    allocation.allocated_seats,
    allocation.required_invigilators,
    allocation.partition_by,
    allocation.partition_value,
    -- joined directly instead of via a person CTE, which would make the planner underestimate the rooms and rescan them per appointment
    coalesce(json_agg(schedule.identity_ref(invigilator)
      ORDER BY invigilator_position.position) FILTER(WHERE invigilator.id IS NOT NULL), '[]') AS invigilators
  FROM
    allocation
    LEFT JOIN LATERAL unnest(allocation.invigilators)
    WITH ORDINALITY AS invigilator_position(id, position) ON TRUE
    LEFT JOIN core.identity invigilator ON invigilator.id = invigilator_position.id
  GROUP BY
    allocation.exam_entry,
    allocation.room,
    allocation.allocated_seats,
    allocation.required_invigilators,
    allocation.partition_by,
    allocation.partition_value
),
entry_rooms AS(
  SELECT
    allocation.exam_entry,
    json_agg(
      CASE WHEN p_plan_draft IS NULL THEN
        json_build_object('id', room.id, 'label', room.label, 'abbrev', room.abbrev, 'invigilators', allocation.invigilators, 'partitionBy', allocation.partition_by, 'partitionValue', allocation.partition_value)
      ELSE
        json_build_object('id', room.id, 'label', room.label, 'abbrev', room.abbrev, 'invigilators', allocation.invigilators, 'partitionBy', allocation.partition_by, 'partitionValue', allocation.partition_value, 'examCapacityOpt', room.exam_capacity_opt, 'examCapacityMax', room.exam_capacity_max, 'allocatedSeats', allocation.allocated_seats, 'requiredInvigilators', allocation.required_invigilators)
      END ORDER BY room.abbrev, room.id) AS rooms
  FROM
    allocation_invigilators allocation
    JOIN schedule.room room ON room.id = allocation.room
  GROUP BY
    allocation.exam_entry
)
SELECT
  coalesce(json_agg(
      CASE WHEN p_plan_draft IS NULL THEN
        json_build_object('id', selected.id, 'start', selected."start", 'end', selected."end", 'examName', exam.name, 'assessmentMethod', selected.assessment_method, 'modules', coalesce(exam_modules.modules, '[]'), 'po', coalesce(exam_po.po, '[]'), 'rooms', coalesce(entry_rooms.rooms, '[]'), 'teachingUnits', coalesce(exam_teaching_units.teaching_units, '{}'))
      ELSE
        json_build_object('id', selected.id, 'start', selected."start", 'end', selected."end", 'examName', exam.name, 'assessmentMethod', selected.assessment_method, 'modules', coalesce(exam_modules.modules, '[]'), 'po', coalesce(exam_po.po, '[]'), 'rooms', coalesce(entry_rooms.rooms, '[]'), 'exam', selected.exam, 'phase', selected.phase, 'expectedParticipants', selected.expected_participants)
      END ORDER BY selected."start", selected.id), '[]')::jsonb
FROM
  selected
  JOIN exams.exam exam ON exam.id = selected.exam
  LEFT JOIN exam_modules ON exam_modules.exam = selected.exam
  LEFT JOIN exam_teaching_units ON exam_teaching_units.exam = selected.exam
  LEFT JOIN exam_po ON exam_po.exam = selected.exam
  LEFT JOIN entry_rooms ON entry_rooms.exam_entry = selected.id
$$;

-- Returns whether the exam appointments of an exam plan draft or their room
-- allocations differ from their published copies, including appointments that
-- were added or removed since the last publication. A row without counterpart
-- differs because its compared key is NULL on the missing side.
CREATE OR REPLACE FUNCTION schedule.exam_plan_has_unpublished_changes(p_plan_draft uuid)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  AS $$
  WITH draft_entry AS(
    SELECT
      *
    FROM
      schedule.exam_entry_draft draft
    WHERE
      draft.plan_draft = p_plan_draft
),
live_entry AS(
  SELECT
    *
  FROM
    schedule.exam_entry live
  WHERE
    live.source_plan_draft = p_plan_draft
),
-- materialized, otherwise the planner loops over the live room join below the full join
draft_room AS MATERIALIZED(
  SELECT
    allocation.*
  FROM
    schedule.exam_room_draft allocation
    JOIN draft_entry ON draft_entry.id = allocation.exam_entry_draft
),
live_room AS MATERIALIZED(
  SELECT
    live_entry.source_exam_entry_draft,
    allocation.*
  FROM
    schedule.exam_room allocation
    JOIN live_entry ON live_entry.id = allocation.exam_entry
)
SELECT
  EXISTS(
    SELECT
    FROM
      draft_entry
      FULL JOIN live_entry ON live_entry.source_exam_entry_draft = draft_entry.id
    WHERE(draft_entry.id, draft_entry.exam, draft_entry.phase, draft_entry.assessment_method, draft_entry."start", draft_entry."end") IS DISTINCT FROM(live_entry.source_exam_entry_draft,
      live_entry.exam,
      live_entry.phase,
      live_entry.assessment_method,
      live_entry."start",
      live_entry."end"))
  OR EXISTS(
    SELECT
    FROM
      draft_room
    FULL JOIN live_room ON live_room.source_exam_entry_draft = draft_room.exam_entry_draft
      AND live_room.room = draft_room.room
  WHERE(draft_room.exam_entry_draft, draft_room.room, draft_room.invigilators, draft_room.partition_by, draft_room.partition_value) IS DISTINCT FROM(live_room.source_exam_entry_draft,
    live_room.room,
    live_room.invigilators,
    live_room.partition_by,
    live_room.partition_value))
$$;

-- Publishes the exam appointments of an exam plan draft with their room
-- allocations: published copies of removed drafts are deleted, those of
-- changed drafts updated and new drafts copied. Only published rows that
-- change are written or locked, so ids of published appointments stay stable.
-- The planned exams are locked against merges. Sets the publication
-- time of the plan draft and returns the number of updated plan drafts. The
-- caller locks the plan draft row.
CREATE OR REPLACE FUNCTION schedule.publish_exam_plan(p_plan_draft uuid)
  RETURNS integer
  LANGUAGE sql
  AS $$
  -- waits for concurrent merges of the planned exams, whose sources the following
  -- statements then no longer see, as each one reads the latest committed state
  SELECT
  FROM
    exams.exam exam
  WHERE
    exam.id IN(
      SELECT
        draft.exam
      FROM
        schedule.exam_entry_draft draft
      WHERE
        draft.plan_draft = p_plan_draft)
  ORDER BY
    exam.id
  FOR KEY SHARE;
  DELETE FROM schedule.exam_entry live
  WHERE live.source_plan_draft = p_plan_draft
    AND NOT EXISTS(
      SELECT
      FROM
        schedule.exam_entry_draft draft
      WHERE
        draft.id = live.source_exam_entry_draft
        AND draft.plan_draft = p_plan_draft);
  INSERT INTO schedule.exam_entry AS live(id, semester, exam, phase, assessment_method, "start", "end", source_plan_draft, source_exam_entry_draft)
  SELECT
    gen_random_uuid(),
    plan.semester,
    draft.exam,
    draft.phase,
    draft.assessment_method,
    draft."start",
    draft."end",
    plan.id,
    draft.id
  FROM
    schedule.exam_entry_draft draft
    JOIN schedule.plan_draft plan ON plan.id = draft.plan_draft
  WHERE
    draft.plan_draft = p_plan_draft
    -- unchanged copies are skipped, as ON CONFLICT would lock them
    AND NOT EXISTS(
      SELECT
      FROM
        schedule.exam_entry published
      WHERE
        published.source_exam_entry_draft = draft.id
        AND(published.semester,
          published.exam,
          published.phase,
          published.assessment_method,
          published."start",
          published."end") IS NOT DISTINCT FROM(plan.semester,
          draft.exam,
          draft.phase,
          draft.assessment_method,
          draft."start",
          draft."end"))
  ON CONFLICT(source_exam_entry_draft)
WHERE
  source_exam_entry_draft IS NOT NULL
    DO UPDATE SET
      semester = excluded.semester,
      exam = excluded.exam,
      phase = excluded.phase,
      assessment_method = excluded.assessment_method,
      "start" = excluded."start",
      "end" = excluded."end";
      DELETE FROM schedule.exam_room live_room USING schedule.exam_entry live
WHERE live.id = live_room.exam_entry
  AND live.source_plan_draft = p_plan_draft
  AND NOT EXISTS(
    SELECT
    FROM schedule.exam_room_draft draft_room
    WHERE
      draft_room.exam_entry_draft = live.source_exam_entry_draft
      AND draft_room.room = live_room.room);
INSERT INTO schedule.exam_room AS live_room(exam_entry, room, invigilators, partition_by, partition_value)
SELECT
  live.id,
  draft_room.room,
  draft_room.invigilators,
  draft_room.partition_by,
  draft_room.partition_value
FROM
  schedule.exam_room_draft draft_room
  JOIN schedule.exam_entry live ON live.source_exam_entry_draft = draft_room.exam_entry_draft
WHERE
  live.source_plan_draft = p_plan_draft
  -- unchanged allocations are skipped, as ON CONFLICT would lock them
EXCEPT
SELECT
  published_room.exam_entry,
  published_room.room,
  published_room.invigilators,
  published_room.partition_by,
  published_room.partition_value
FROM
  schedule.exam_room published_room
  JOIN schedule.exam_entry live ON live.id = published_room.exam_entry
WHERE
  live.source_plan_draft = p_plan_draft
ON CONFLICT(exam_entry,
  room)
  DO UPDATE SET
    invigilators = excluded.invigilators,
    partition_by = excluded.partition_by,
    partition_value = excluded.partition_value;
    UPDATE
      schedule.plan_draft
    SET
      published_at = LOCALTIMESTAMP
    WHERE
      id = p_plan_draft
    RETURNING
      1;
$$;

