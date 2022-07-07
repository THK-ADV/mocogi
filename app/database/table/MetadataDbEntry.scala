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
    recommendedSemester: Int,
    season: String,
    workloadTotal: Int,
    workloadLecture: Int,
    workloadSeminar: Int,
    workloadPractical: Int,
    workloadExercise: Int,
    workloadSelfStudy: Int,
    recommendedPrerequisites: String,
    requiredPrerequisites: String,
    status: String,
    location: String,
    po: String
)
