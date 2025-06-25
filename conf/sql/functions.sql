DROP FUNCTION IF EXISTS identity_to_json (IDENTITY) CASCADE;

DROP FUNCTION IF EXISTS module_to_json_short (module) CASCADE;

DROP FUNCTION IF EXISTS resolve_prereqs (jsonb) CASCADE;

DROP FUNCTION IF EXISTS resolve_responsibilities (uuid) CASCADE;

DROP FUNCTION IF EXISTS resolve_assessment_methods (uuid) CASCADE;

DROP FUNCTION IF EXISTS resolve_po_relationships (uuid) CASCADE;

DROP FUNCTION IF EXISTS resolve_taught_with (uuid) CASCADE;

DROP FUNCTION IF EXISTS resolve_module_relation (uuid) CASCADE;

DROP FUNCTION IF EXISTS get_module_details (uuid) CASCADE;

DROP FUNCTION IF EXISTS get_modules_for_user (text) CASCADE;

DROP FUNCTION IF EXISTS get_user_info (text, text) CASCADE;

CREATE OR REPLACE FUNCTION identity_to_json (i IDENTITY)
    RETURNS jsonb
    LANGUAGE sql
    IMMUTABLE
    AS $$
    SELECT
        CASE WHEN i.kind = 'person' THEN
            jsonb_build_object('id', i.id, 'kind', i.kind, 'title', i.title, 'lastname', i.lastname, 'firstname', i.firstname, 'faculties', i.faculties, 'imageUrl', i.image_url, 'isActive', i.is_active, 'websiteUrl', i.website_url, 'abbreviation', i.abbreviation, 'employmentType', i.employment_type)
        ELSE
            -- other kinds are 'group' or 'unknown'
            jsonb_build_object('id', i.id, 'title', i.title, 'isActive', i.is_active, 'kind', i.kind)
        END;
$$;

CREATE OR REPLACE FUNCTION module_to_json_short (m module)
    RETURNS jsonb
    LANGUAGE sql
    IMMUTABLE
    AS $$
    SELECT
        jsonb_build_object('id', m.id, 'title', m.title, 'abbreviation', m.abbrev);
$$;

CREATE OR REPLACE FUNCTION resolve_prereqs (prerequisites jsonb)
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
                        jsonb_agg(module_to_json_short (module))
                    FROM jsonb_array_elements_text(prerequisites -> 'modules') AS arr (mid)
                    JOIN module ON module.id = arr.mid::uuid), '[]'::jsonb))
        END;
$$;

CREATE OR REPLACE FUNCTION resolve_responsibilities (module_id uuid)
    RETURNS TABLE (
        module_management jsonb,
        lecturer jsonb)
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        coalesce(jsonb_agg(identity_to_json (i)) FILTER (WHERE mr.responsibility_type = 'module_management'), '[]'::jsonb) AS module_management,
        coalesce(jsonb_agg(identity_to_json (i)) FILTER (WHERE mr.responsibility_type = 'lecturer'), '[]'::jsonb) AS lecturer
    FROM
        module_responsibility AS mr
        JOIN IDENTITY AS i ON i.id = mr.identity
    WHERE
        mr.module = module_id;
$$;

CREATE OR REPLACE FUNCTION resolve_assessment_methods (module_id uuid)
    RETURNS jsonb
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        coalesce(jsonb_agg(jsonb_build_object('label', am.de_label, 'source', am.source, 'percentage', mam.percentage, 'preconditions', coalesce((
                        SELECT
                            jsonb_agg(pre_am.de_label)
                        FROM unnest(mam.precondition) AS pre_id (id)
                        JOIN assessment_method AS pre_am ON pre_am.id = pre_id.id), '[]'::jsonb))), '[]'::jsonb)
    FROM
        module_assessment_method AS mam
        JOIN assessment_method AS am ON am.id = mam.assessment_method
    WHERE
        mam.module = module_id;
$$;

CREATE OR REPLACE FUNCTION resolve_po_relationships (module_id uuid)
    RETURNS TABLE (
        po_mandatory jsonb,
        po_optional jsonb)
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        -- PO Mandatory
        coalesce((
            SELECT
                jsonb_agg(jsonb_build_object('poId', po.id, 'poVersion', po.version, 'studyProgramLabel', sp.de_label, 'studyProgramAbbreviation', sp.abbreviation, 'degree', deg.de_label, 'specialization', spec.label, 'recommendedSemester', mpm.recommended_semester))
            FROM module_po_mandatory AS mpm
            JOIN po ON po.id = mpm.po
            JOIN study_program AS sp ON sp.id = po.study_program
            JOIN degree AS deg ON deg.id = sp.degree
            LEFT JOIN specialization AS spec ON spec.id = mpm.specialization
            WHERE
                mpm.module = module_id), '[]'::jsonb) AS po_mandatory,
        -- PO Optional
        coalesce((
            SELECT
                jsonb_agg(jsonb_build_object('poId', po.id, 'poVersion', po.version, 'studyProgramLabel', sp.de_label, 'studyProgramAbbreviation', sp.abbreviation, 'degree', deg.de_label, 'specialization', spec.label, 'recommendedSemester', mpo.recommended_semester, 'instanceOf', module_to_json_short (inst_mod)))
            FROM module_po_optional AS mpo
            JOIN po ON po.id = mpo.po
            JOIN study_program AS sp ON sp.id = po.study_program
            JOIN degree AS deg ON deg.id = sp.degree
            LEFT JOIN specialization AS spec ON spec.id = mpo.specialization
            JOIN module AS inst_mod ON inst_mod.id = mpo.instance_of
            WHERE
                mpo.module = module_id), '[]'::jsonb) AS po_optional;
$$;

CREATE OR REPLACE FUNCTION resolve_taught_with (module_id uuid)
    RETURNS jsonb
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        coalesce(jsonb_agg(module_to_json_short (m)), '[]'::jsonb)
    FROM
        module_taught_with AS mtw
        JOIN module AS m ON m.id = mtw.module_taught
    WHERE
        mtw.module = module_id;
$$;

CREATE OR REPLACE FUNCTION resolve_module_relation (module_id uuid)
    RETURNS jsonb
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        CASE
        -- Check if this module is a child (has a parent)
        WHEN EXISTS (
            SELECT
                1
            FROM
                module_relation
            WHERE
                module = module_id
                AND relation_type = 'child') THEN
            jsonb_build_object('relationType', 'child', 'module', (
                    SELECT
                        module_to_json_short (m)
                    FROM module_relation AS mr
                    JOIN module AS m ON m.id = mr.relation_module
                    WHERE
                        mr.module = module_id
                        AND mr.relation_type = 'child' LIMIT 1))
            -- Check if this module is a parent (has children)
        WHEN EXISTS (
            SELECT
                1
            FROM
                module_relation
            WHERE
                module = module_id
                AND relation_type = 'parent') THEN
            jsonb_build_object('relationType', 'parent', 'modules', coalesce((
                    SELECT
                        jsonb_agg(module_to_json_short (m))
                    FROM module_relation AS mr
                    JOIN module AS m ON m.id = mr.relation_module
                WHERE
                    mr.module = module_id
                    AND mr.relation_type = 'parent'), '[]'::jsonb))
            -- No relations found
        ELSE
            NULL
        END;
$$;

CREATE OR REPLACE FUNCTION get_module_details (module_id uuid)
    RETURNS jsonb
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        jsonb_build_object('id', b.id, 'lastModified', b.last_modified, 'title', b.title, 'abbreviation', b.abbrev, 'moduleType', b.moduletypelabel, 'ects', b.ects, 'language', b.languagelabel, 'duration', b.duration, 'season', b.seasonlabel, 'workload', b.workload, 'status', b.statuslabel, 'location', b.locationlabel, 'firstExaminer', b.firstexaminer, 'secondExaminer', b.secondexaminer, 'examPhases', b.exam_phases, 'participants', b.participants, 'recommendedPrerequisites', resolve_prereqs (b.recommended_prerequisites), 'requiredPrerequisites', resolve_prereqs (b.required_prerequisites), 'content', jsonb_build_object('learningOutcome', b.learning_outcome_de, 'moduleContent', b.module_content_de, 'learningMethods', b.learning_methods_de, 'literature', b.literature_de, 'particularities', b.particularities_de), 'moduleManagement', coalesce(resp.module_management, '[]'::jsonb), 'lecturer', coalesce(resp.lecturer, '[]'::jsonb), 'assessments', resolve_assessment_methods (b.id), 'poMandatory', po.po_mandatory, 'poOptional', po.po_optional, 'taughtWith', resolve_taught_with (b.id), 'moduleRelation', resolve_module_relation (b.id))
    FROM (
        SELECT
            m.*,
            mt.de_label AS moduletypelabel,
            lng.de_label AS languagelabel,
            ssn.de_label AS seasonlabel,
            sts.de_label AS statuslabel,
            loc.de_label AS locationlabel,
            identity_to_json (fe) AS firstexaminer,
            identity_to_json (se) AS secondexaminer
        FROM
            module AS m
            JOIN module_type AS mt ON mt.id = m.module_type
            JOIN
            LANGUAGE AS
            lng ON lng.id = m.language
            JOIN season AS ssn ON ssn.id = m.season
            JOIN status AS sts ON sts.id = m.status
            JOIN location AS loc ON loc.id = m.location
            JOIN IDENTITY AS fe ON fe.id = m.first_examiner
            JOIN IDENTITY AS se ON se.id = m.second_examiner
        WHERE
            m.id = module_id) AS b
    LEFT JOIN LATERAL resolve_responsibilities (b.id) AS resp ON TRUE
    LEFT JOIN LATERAL resolve_po_relationships (b.id) AS po ON TRUE;
$$;

-- Returns modules for a given campus with their draft states and permissions
-- @param campus_id_param: The campus ID to filter modules for
-- @returns: JSON array of modules with their metadata, draft state, and permissions
CREATE OR REPLACE FUNCTION get_modules_for_user (campus_id_param text)
    RETURNS jsonb
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        coalesce(json_agg(json_build_object('isPrivilegedForModule', p.kind = 'inherited', 'isNewModule', cm.module IS NOT NULL, 'module', json_build_object('id', coalesce(m.id, cm.module), 'title', coalesce(m.title, cm.module_title), 'abbreviation', coalesce(m.abbrev, cm.module_abbrev)), 'ects', coalesce((md.module_json -> 'metadata' -> 'ects')::numeric, m.ects, cm.module_ects), 'mandatoryPOs', cm.module_mandatory_pos, 'moduleDraftState', CASE WHEN md.module IS NULL THEN
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
                END, 'moduleDraft', CASE WHEN md.module IS NOT NULL THEN
                    json_build_object('id', md.module, 'title', md.module_title, 'abbreviation', md.module_abbrev, 'modifiedKeys', md.modified_keys, 'keysToBeReviewed', md.keys_to_be_reviewed)
                END))::jsonb, '[]'::jsonb)
    FROM
        module_update_permission p
    LEFT JOIN module m ON p.module = m.id
    LEFT JOIN created_module_in_draft cm ON p.module = cm.module
    LEFT JOIN module_draft md ON p.module = md.module
WHERE
    p.campus_id = campus_id_param
    AND ((m.id IS NOT NULL
            AND cm.module IS NULL)
        OR (m.id IS NULL
            AND cm.module IS NOT NULL));
$$;

CREATE OR REPLACE FUNCTION get_user_info (uid text, cid text)
    RETURNS jsonb
    LANGUAGE sql
    STABLE
    AS $$
    SELECT
        jsonb_build_object('hasUniversityRole', EXISTS (
                SELECT
                    1
                FROM study_program_person p
                WHERE
                    p.person = uid), 'hasModulesToEdit', EXISTS (
                SELECT
                    1
                FROM module_update_permission m
                WHERE
                    m.campus_id = cid), 'rejectedReviews', (
                SELECT
                    count(*)
                FROM module_update_permission mp
                JOIN module_review mr ON mp.module = mr.module_draft
            WHERE
                mp.campus_id = cid
                AND mr.status = 'rejected'), 'reviewsToApprove', (
                SELECT
                    count(*)
                FROM study_program_person sp
                JOIN module_review mr ON sp.study_program = mr.study_program
                    AND sp.role = mr.role
                    AND mr.status = 'pending'
            WHERE
                sp.person = uid))
$$;

