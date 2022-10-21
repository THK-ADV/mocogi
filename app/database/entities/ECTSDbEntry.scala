package database.entities

import java.util.UUID

case class ECTSDbEntry(id: UUID, value: Double)

case class ECTSFocusAreaContributionDbEntry(
    id: UUID,
    ects: UUID,
    focusArea: UUID,
    ectsValue: Double,
    description: String
)
