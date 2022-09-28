package parsing.types

case class AssessmentMethods(
    mandatory: List[AssessmentMethodEntry],
    optional: List[AssessmentMethodEntry]
)
