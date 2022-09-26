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
    recommendedSemester: Int,
    frequency: Season,
    responsibilities: Responsibilities,
    assessmentMethods: List[AssessmentMethodPercentage],
    workload: Workload,
    recommendedPrerequisites: List[String],
    requiredPrerequisites: List[String],
    status: Status,
    location: Location,
    po: List[String]
)
