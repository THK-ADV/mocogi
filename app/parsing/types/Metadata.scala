package parsing.types

import java.util.UUID

case class Metadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    relation: Option[ModuleRelation],
    credits: ECTS,
    language: Language,
    duration: Int,
    frequency: Season,
    responsibilities: Responsibilities,
    assessmentMethodsMandatory: List[AssessmentMethodEntry],
    assessmentMethodsOptional: List[AssessmentMethodEntry],
    workload: Workload,
    recommendedPrerequisites: Option[Prerequisites],
    requiredPrerequisites: Option[Prerequisites],
    status: Status,
    location: Location,
    poMandatory: List[POMandatory],
    poOptional: List[POOptional],
    participants: Option[Participants],
    competences: List[Competence],
    globalCriteria: List[GlobalCriteria]
)
