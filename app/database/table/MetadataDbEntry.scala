package database.table

import java.util.UUID

case class MetadataDbEntry(
    id: UUID,
    gitPath: String,
    title: String,
    abbrev: String,
    moduleType: String,
    children: Option[String],
    parent: Option[String],
    credits: Double,
    language: String,
    duration: Int,
    season: String,
    workloadLecture: Int,
    workloadSeminar: Int,
    workloadPractical: Int,
    workloadExercise: Int,
    workloadProjectSupervision: Int,
    workloadProjectWork: Int,
    recommendedPrerequisites: String,
    requiredPrerequisites: String,
    status: String,
    location: String,
    poMandatory: String
)
