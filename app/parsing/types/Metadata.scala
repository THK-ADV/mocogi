package parsing.types

import java.util.UUID

case class Metadata(
    id: UUID,
    title: String,
    abbrev: String,
    kind: ModuleType,
    credits: Double,
    language: Language,
    duration: Int,
    recommendedSemester: Int,
    frequency: Season,
    responsibilities: Responsibilities,
    assessmentMethod: List[AssessmentMethod],
    workload: Workload,
    recommendedPrerequisites: List[String],
    requiredPrerequisites: List[String],
    status: Status,
    location: Location,
    po: List[String]
)
