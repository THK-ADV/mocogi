package database.table

import java.util.UUID

case class AssessmentMethodMetadataDbEntry(
    metadata: UUID,
    assessmentMethod: String,
    percentage: Option[Double]
)
