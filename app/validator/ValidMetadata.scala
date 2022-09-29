package validator

import parsing.types.{AssessmentMethods, ECTS, Participants}
import validator.MetadataValidator.Module

import java.util.UUID

case class ValidMetadata(
    id: UUID,
    assessmentMethods: AssessmentMethods,
    participants: Option[Participants],
    ects: ECTS,
    taughtWith: List[Module]
)
