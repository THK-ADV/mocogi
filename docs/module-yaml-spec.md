# Module YAML specification

This document describes the `v1s` YAML front matter that mocogi parses from and prints to module Markdown files. It reflects the current parser, printer, and metadata validation rules.

## Document structure

Schematically, the metadata starts with a version delimiter and ends with a plain `---` delimiter:

```text
---v1s
<metadata keys in the order specified below>
---
<Markdown module content>
```

`---v1s` denote the only supported version scheme. The opening delimiter must be at the start of the document, the closing delimiter is required, and Markdown module content follows the closing delimiter.

The implementation is an order-sensitive line parser rather than a general YAML object decoder. Keys and nested keys must therefore appear in the order shown below; optional sections may be omitted, and scalar-or-list fields may use either a single value or a dash list. Unknown keys are unsupported and usually cause parsing to fail.

## Top-level keys

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `id` | Yes | Uniquely identifies the module. | `00895144-30e4-4bd2-b800-bb706686d950` | Must be a syntactically valid UUID (version 4). |
| `title` | Yes | Gives the module's display title. | `Algorithmik` | Must be a non-empty single-line string after trimming. |
| `abbreviation` | Yes | Gives the short module code used in compact displays. | `ALG` | Must be a non-empty single-line string after trimming. |
| `type` | Yes | Selects whether the entry is a regular or generic module. | `type.module` | Must be one of the [module types](#module-types) currently present in `core.module_type`. |
| `relation` | No | Declares that this module is a parent whose `children` are submodules. | A `children` block containing `module.<uuid>` references | When present it must contain at least one child, and the resulting relation graph must satisfy the [relation rules](#relation). |
| `ects` | Yes | Specifies the module's ECTS credits. | `5` or `7.5` | Must be a parseable number and must not be `0`; negative values are not rejected directly but can pass full validation only when the module has no PO assignments. |
| `language` | Yes | Identifies the teaching language or language combination. | `lang.de` | Must be one of the [languages](#languages) currently present in `core.language`. |
| `duration` | Yes | Gives the planned duration in semesters. | `1` | Must be an integer greater than or equal to `0`. |
| `frequency` | Yes | Identifies the semester pattern in which the module is offered. | `season.ws` | Must be one of the [seasons](#seasons) currently present in `core.season`. |
| `responsibilities` | Yes | Groups the people responsible for and teaching the module. | A `module_management` and `lecturers` block | Both nested keys are required and each must resolve to at least one [identity](#identities). |
| `assessment_methods_mandatory` | No | Lists the assessment methods combined as mandatory components of the module assessment. | One `method: assessment.written-exam` entry | Entries must follow the [assessment method rules](#assessment-methods); omission represents an empty list. |
| `first_examiner` | Yes | Identifies the first examiner. | `person.ald` or `person.nn` | Must be an [identity](#identities). |
| `second_examiner` | Yes | Identifies the second examiner. | `person.ald` or `person.nn` | Must be an [identity](#identities). |
| `exam_phases` | Yes | Lists the exam scheduling phases in which the assessment may occur. | `exam_phase.wise_1` | Must contain one or more [exam phases](#exam-phases). |
| `workload` | Yes | Breaks the taught workload down into six categories measured in hours. | A block such as `lecture: 36` and `exercise: 18` | All six nested integer keys are required in order and must satisfy the [workload rules](#workload). |
| `recommended_prerequisites` | No | Describes knowledge or modules recommended before taking this module. | `text: Grundkenntnisse der Programmierung` | May contain `text`, `modules`, or both, in that order, and referenced modules must exist. |
| `required_prerequisites` | No | Describes knowledge or modules that are formally required before taking this module. | `modules: module.<uuid>` | May contain `text`, `modules`, or both, in that order, and referenced modules must exist. |
| `status` | Yes | Indicates whether the module is currently active. | `status.active` | Must be one of the [statuses](#statuses) currently present in `core.status`. |
| `location` | Yes | Identifies the campus or delivery location. | `location.gm` | Must be one of the [locations](#locations) currently present in `core.location`. |
| `po_mandatory` | No | Lists examination regulations in which the module is mandatory. | A `study_program: study_program.inf_mi5` entry | Each entry must satisfy the [mandatory PO rules](#mandatory-po-entries); omission represents an empty list. |
| `po_optional` | No | Lists examination regulations in which this module instantiates an optional generic module. | A `study_program: study_program.inf_mi5` entry with `instance_of` | Each entry must satisfy the [optional PO rules](#optional-po-entries); omission represents an empty list. |
| `participants` | No | Defines the allowed participant-count range. | `min: 4` and `max: 20` | Both integers are required, both must be non-negative, and `min` must be strictly less than `max`. |
| `taught_with` | No | Lists other modules taught together with this one. | `module.330bd356-e766-433b-ae2c-0a98d49ca49d` | Accepts one UUID reference or a dash list, and every referenced module must exist. |
| `attendance_requirement` | No | Records an exceptional attendance requirement for admission to an assessment. | A block describing a minimum, reason, and handling of absence | All three nested single-line string keys are required in order; no additional content validation is applied. |
| `assessment_prerequisite` | No | Records an exceptional prerequisite assessment for admission to the module assessment. | A block describing affected modules and a reason | Both nested single-line string keys are required in order; no module-reference validation is applied because `modules` is free text. |

## Nested keys and rules

### Relation

```yaml
relation:
  children:
    - module.0ef7d976-206e-4654-a987-8d0e184fdd3f
    - module.330bd356-e766-433b-ae2c-0a98d49ca49d
```

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `relation.children` | Yes within `relation` | Lists the direct submodules owned by this parent module. | `module.<uuid>` | Accepts one value or a non-empty dash list; every UUID must resolve to a module, duplicates and self-references are rejected, a child may have only one parent, and no module may be both a parent and a child. |

### Responsibilities

```yaml
responsibilities:
  module_management: person.ald
  lecturers:
    - person.abr
    - person.ald
```

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `responsibilities.module_management` | Yes | Lists the people or groups that manage the module. | `person.ald` | Accepts one value or a non-empty dash list, and every value must be an [identity](#identities). |
| `responsibilities.lecturers` | Yes | Lists the people or groups that teach the module. | `person.abr` | Accepts one value or a non-empty dash list, and every value must be an [identity](#identities). |

### Assessment methods

```yaml
assessment_methods_mandatory:
  - method: assessment.written-exam
    percentage: 60
  - method: assessment.project
    percentage: 40
```

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `assessment_methods_mandatory[].method` | Yes per entry | Identifies one mandatory assessment component. | `assessment.written-exam` | Must be one of the [assessment methods](#assessment-method-values) currently present in `core.assessment_method`. |
| `assessment_methods_mandatory[].percentage` | No | Assigns the component's share of the overall result. | `100`, or `60` and `40` across two entries | Must be a parseable number; missing percentages count as zero, and the sum across all entries must be exactly `0` or `100`. |

### Workload

```yaml
workload:
  lecture: 36
  seminar: 0
  practical: 18
  exercise: 18
  project_supervision: 0
  project_work: 0
```

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `workload.lecture` | Yes | Gives lecture contact hours. | `36` | Must be an integer greater than or equal to `0`. |
| `workload.seminar` | Yes | Gives seminar contact hours. | `0` | Must be an integer greater than or equal to `0`. |
| `workload.practical` | Yes | Gives practical or laboratory hours. | `18` | Must be an integer greater than or equal to `0`. |
| `workload.exercise` | Yes | Gives exercise or tutorial hours. | `18` | Must be an integer greater than or equal to `0`. |
| `workload.project_supervision` | Yes | Gives supervised project hours. | `0` | Must be an integer greater than or equal to `0`. |
| `workload.project_work` | Yes | Gives independent project-work hours. | `0` | Must be an integer greater than or equal to `0`. |

When the module has PO assignments, the sum of the six workload values must not exceed `ects × ects_factor`, using the smallest `ects_factor` among its referenced POs. No upper-bound check is applied when the module has no PO assignments.

### Prerequisites

```yaml
recommended_prerequisites:
  text: Grundkenntnisse der Programmierung
  modules:
    - module.ce09539e-fc0a-4c74-b85d-40a293998bb4
```

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `recommended_prerequisites.text` | No | Describes recommended prior knowledge as free text. | `Grundkenntnisse der Programmierung` | Accepts a single line or a plain, `>`, or `|` indented multiline block, and an empty value is allowed; use a single line when parser/printer round-tripping is required because the printer does not re-emit multiline values as indented blocks. |
| `recommended_prerequisites.modules` | No | Lists modules recommended as prior study. | `module.<uuid>` | Accepts one value or a dash list, and every value must be a valid UUID resolving to an existing module. |
| `required_prerequisites.text` | No | Describes formally required prior knowledge as free text. | `Bestandene Grundlagenmodule` | Accepts a single line or a plain, `>`, or `|` indented multiline block, and an empty value is allowed; use a single line when parser/printer round-tripping is required because the printer does not re-emit multiline values as indented blocks. |
| `required_prerequisites.modules` | No | Lists modules required as prior study. | `module.<uuid>` | Accepts one value or a dash list, and every value must be a valid UUID resolving to an existing module. |

### Mandatory PO entries

```yaml
po_mandatory:
  - study_program: study_program.inf_mi5
    recommended_semester:
      - 1
      - 2
    recommended_semester_part_time: 3
```

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `po_mandatory[].study_program` | Yes per entry | Identifies the PO, optionally narrowed to a specialization, in which the module is mandatory. | `study_program.inf_mi5` | The PO ID must be one of the [POs](#pos); an optional second suffix must be a [specialization ID](#specializations), although the current parser does not verify that the specialization belongs to the selected PO. |
| `po_mandatory[].recommended_semester` | No | Lists the full-time semesters in which the module is normally taken. | `1` or a dash list containing `1` and `2` | Accepts one integer or a dash list of integers; no range, positivity, uniqueness, or ordering validation is applied. |
| `po_mandatory[].recommended_semester_part_time` | No | Gives the semester used for the alternative part-time study plan. | `3` | Must be a single integer; no range or positivity validation is applied. |

### Optional PO entries

```yaml
po_optional:
  - study_program: study_program.inf_mi5
    instance_of: module.d1cecfbc-a314-42f6-99b3-be92f22c3295
    recommended_semester: 3
```

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `po_optional[].study_program` | Yes per entry | Identifies the PO, optionally narrowed to a specialization, in which the module is an elective. | `study_program.inf_mi5` | The PO ID must be one of the [POs](#pos); an optional second suffix must be a [specialization ID](#specializations), although the current parser does not verify that the specialization belongs to the selected PO. |
| `po_optional[].instance_of` | Yes per entry | Identifies the generic module instantiated by this concrete elective. | `module.<uuid>` | Must be a valid UUID resolving to an existing module; the current validator does not check that the target has `type.generic_module`. |
| `po_optional[].recommended_semester` | No | Lists semesters in which the elective is normally taken. | `3` | Accepts one integer or a dash list of integers; no range, positivity, uniqueness, or ordering validation is applied. |

### Participants

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `participants.min` | Yes within `participants` | Gives the minimum number of participants. | `4` | Must be a non-negative integer strictly smaller than `participants.max`. |
| `participants.max` | Yes within `participants` | Gives the maximum number of participants. | `20` | Must be a non-negative integer strictly greater than `participants.min`. |

### Attendance requirement

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `attendance_requirement.min` | Yes within `attendance_requirement` | States the minimum attendance needed for admission to the assessment. | `5 von 7 Terminen` | Must be a single-line string; the parser does not enforce a numeric format or non-emptiness. |
| `attendance_requirement.reason` | Yes within `attendance_requirement` | Explains why attendance is required. | `Praktische Übungen bauen aufeinander auf` | Must be a single-line string; no additional validation is applied. |
| `attendance_requirement.absence` | Yes within `attendance_requirement` | Explains how absences are handled. | `Ein Fehltermin kann nachgeholt werden` | Must be a single-line string; no additional validation is applied. |

### Assessment prerequisite

| Key | Required | Description | Typical value | Validation rules |
| --- | --- | --- | --- | --- |
| `assessment_prerequisite.modules` | Yes within `assessment_prerequisite` | Names the module or module components affected by the prerequisite assessment. | `Praktikum` | Must be a single-line free-text string; UUIDs and module existence are not checked. |
| `assessment_prerequisite.reason` | Yes within `assessment_prerequisite` | Explains the prerequisite assessment. | `Erfolgreicher Praktikumsabschluss` | Must be a single-line string; no additional validation is applied. |

## Allowed values

This section denote values used for variables

### Module types

| YAML value | Meaning |
| --- | --- |
| `type.generic_module` | Generic module / placeholder |
| `type.module` | Regular module |

All values: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/module_type.yaml

### Languages

| YAML value | Meaning |
| --- | --- |
| `lang.de` | German |
| `lang.de_en` | German and English |
| `lang.de_or_en` | German or English |
| `lang.en` | English |

All values: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/lang.yaml

### Seasons

| YAML value | Meaning |
| --- | --- |
| `season.ss` | Summer semester |
| `season.ws` | Winter semester |
| `season.ws_or_ss` | Winter or summer semester |
| `season.ws_ss` | Winter and summer semester |

All values: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/season.yaml

### Statuses

| YAML value | Meaning |
| --- | --- |
| `status.active` | Active |
| `status.inactive` | Inactive |

All values: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/status.yaml

### Locations

| YAML value | Meaning |
| --- | --- |
| `location.dz` | Deutz |
| `location.gm` | Gummersbach |
| `location.km` | Cologne-Mühlheim |
| `location.other` | Other or variable |
| `location.remote` | Remote or online |
| `location.su` | Südstadt |

All values: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/location.yaml

### Assessment method values

| YAML value | Meaning |
| --- | --- |
| `assessment.admission-colloquium` | Zugangskolloquium |
| `assessment.certificate-achievement` | Testat/Zwischentestat |
| `assessment.e-exam` | E-Prüfung |
| `assessment.home-assignment` | Hausarbeit |
| `assessment.open-book-exam` | Open-Book-Ausarbeitung |
| `assessment.oral-contribution` | Mündlicher Beitrag |
| `assessment.oral-exam` | Mündliche Prüfung |
| `assessment.performance-assessment` | Performanzprüfung |
| `assessment.portfolio` | Lernportfolio |
| `assessment.practical-report` | Praktikumsbericht |
| `assessment.project` | Projektarbeit |
| `assessment.role-play` | Rollenspiel |
| `assessment.specimen` | Präparat |
| `assessment.written-exam` | Klausurarbeit |
| `assessment.written-exam-answer-choice-method` | Schriftliche Prüfung im Antwortwahlverfahren |

### Identities

| YAML value | Meaning |
| --- | --- |
| `person.all-inf` | Alle Lehrenden der Lehreinheit Informatik |
| `person.all-mi` | Alle Lehrenden im Studiengang "Medieninformatik (Bachelor/Master)" |
| `person.ado` | Alexander Dobrynin |
| `person.cko` | Prof. Dr. Christian Kohls |
| `person.uvh` | Prof. Dr. Uwe van Heesch |

The set also includes groups and the `person.nn` placeholder. All values: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/person.yaml

### POs

Structure of a PO value:

- `study_program.` is the fixed namespace.
- `inf_mi` is the study program ID which can be found here: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/program.yaml
- `5` is the PO number which can be found here: https://gitlab.git.nrw/thk-f10/cops/module/-/raw/main/core/po.yaml

| YAML value | Meaning |
| --- | --- |
| `study_program.inf_cis1` | Computer and Information Science (M.Sc.), PO 1 |
| `study_program.inf_coco1` | Code & Context (B.Sc.), PO 1 |
| `study_program.inf_dsi1` | Digital Sciences (M.Sc.), PO 1 |
| `study_program.inf_inf2` | Informatik (B.Sc.), PO 2 |
| `study_program.inf_inf3` | Informatik (B.Sc.), PO 3 |
| `study_program.inf_inf1_flex` | Informatik Flexibel (B.Sc.), PO 1 |
| `study_program.inf_itm2` | IT-Management (Informatik) (B.Sc.), PO 2 |
| `study_program.inf_itm3` | IT-Management (Informatik) (B.Sc.), PO 3 |
| `study_program.inf_mi4` | Medieninformatik (B.Sc.), PO 4 |
| `study_program.inf_mi5` | Medieninformatik (B.Sc.), PO 5 |
| `study_program.inf_mi6` | Medieninformatik (B.Sc.), PO 6 |
| `study_program.inf_mim4` | Medieninformatik (M.Sc.), PO 4 |
| `study_program.inf_mim5` | Medieninformatik (M.Sc.), PO 5 |
| `study_program.inf_wi5` | Wirtschaftsinformatik (B.Sc.), PO 5 |
| `study_program.inf_wi6` | Wirtschaftsinformatik (B.Sc.), PO 6 |
| `study_program.inf_wiv2` | Wirtschaftsinformatik, Verbundstudiengang (B.Sc.), PO 2 |
| `study_program.inf_wivm2` | Wirtschaftsinformatik, Verbundstudiengang (M.Sc.), PO 2 |
| `study_program.inf_wsc1` | Web Science (M.Sc.), PO 1 |
| `study_program.ing_ait3` | Automation & IT (M.Eng.), PO 3 |
| `study_program.ing_ait4` | Automation & IT (M.Eng.), PO 4 |
| `study_program.ing_ait5` | Automation & IT (M.Eng.), PO 5 |
| `study_program.ing_een4` | Elektrotechnik (B.Eng.), PO 4 |
| `study_program.ing_een5` | Elektrotechnik (B.Eng.), PO 5 |
| `study_program.ing_gme4` | Maschinenbau (B.Eng.), PO 4 |
| `study_program.ing_gme5` | Maschinenbau (B.Eng.), PO 5 |
| `study_program.ing_pdpd5` | Produktdesign und Prozessentwicklung (M.Sc.), PO 5 |
| `study_program.ing_wiw4` | Wirtschaftsingenieurwesen (B.Eng.), PO 4 |
| `study_program.ing_wiw5` | Wirtschaftsingenieurwesen (B.Eng.), PO 5 |
| `study_program.ing_wiwm2` | Wirtschaftsingenieurwesen (M.Sc.), PO 2 |

### Specializations

Some POs define **Studienschwerpunkte**. In YAML, each "Studienschwerpunkt" is treated as a specialization of the more general PO and is appended to its PO value.

For example, `study_program.ing_gme5.ing_gme5_es` consists of:

- `study_program.` — the fixed namespace.
- `ing_gme5` — the general PO: Maschinenbau, PO 5.
- `ing_gme5_es` — the specialization representing the Studienschwerpunkt "Entwicklung und Simulation".

| YAML value | Meaning |
| --- | --- |
| `study_program.inf_cis1.inf_cis1_itms` | Computer and Information Science, PO 1 — IT-Management und IT-Sicherheit |
| `study_program.inf_mim4.inf_mim4_hci` | Medieninformatik (M.Sc.), PO 4 — Human-Computer Interaction |
| `study_program.inf_mim4.inf_mim4_mppd` | Medieninformatik (M.Sc.), PO 4 — Multiperspective Product Development |
| `study_program.inf_mim4.inf_mim4_sc` | Medieninformatik (M.Sc.), PO 4 — Social Computing |
| `study_program.inf_mim4.inf_mim4_vc` | Medieninformatik (M.Sc.), PO 4 — Visual Computing |
| `study_program.inf_mim4.inf_mim4_wtw` | Medieninformatik (M.Sc.), PO 4 — Weaving the Web |
| `study_program.ing_gme4.ing_gme4_ftg` | Maschinenbau, PO 4 — Fertigung |
| `study_program.ing_gme4.ing_gme4_kst` | Maschinenbau, PO 4 — Konstruktion |
| `study_program.ing_gme4.ing_gme4_ut` | Maschinenbau, PO 4 — Umwelttechnik |
| `study_program.ing_gme5.ing_gme5_es` | Maschinenbau, PO 5 — Entwicklung und Simulation |
| `study_program.ing_gme5.ing_gme5_nt` | Maschinenbau, PO 5 — Nachhaltige Technologien |
| `study_program.ing_gme5.ing_gme5_pf` | Maschinenbau, PO 5 — Produktionstechnik |
| `study_program.ing_wiw4.ing_wiw4_et` | Wirtschaftsingenieurwesen, PO 4 — Elektrotechnik |
| `study_program.ing_wiw4.ing_wiw4_mb` | Wirtschaftsingenieurwesen, PO 4 — Maschinenbau |
| `study_program.ing_wiw4.ing_wiw4_ut` | Wirtschaftsingenieurwesen, PO 4 — Umwelttechnik |

### Exam phases

| YAML value | Meaning |
| --- | --- |
| `exam_phase.wise_1` | Winter Semester Period 1 (Jan.–Apr.) |
| `exam_phase.sose_1` | Summer Semester Period 1 (Jul.) |
| `exam_phase.sose_2` | Summer Semester Period 2 (Sep.–Oct.) |
| `exam_phase.off_wise` | Outside the Winter Semester |
| `exam_phase.off_sose` | Outside the Summer Semester |
| `exam_phase.off_schedule` | Outside the Exam Weeks |
| `exam_phase.none` | To be announced (TBA) |