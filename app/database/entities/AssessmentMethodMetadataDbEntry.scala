package database.entities

import java.util.UUID

case class AssessmentMethodMetadataDbEntry(
    metadata: UUID,
    assessmentMethod: String,
    percentage: Option[Double],
    mandatory: Boolean
)

case class AssessmentMethodMetadataPreconditionDbEntry(
    assessmentMethodMetadata: UUID,
    assessmentMethod: String
)
