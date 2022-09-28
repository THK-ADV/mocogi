package parsing.types

case class AssessmentMethodEntry(
    method: AssessmentMethod,
    percentage: Option[Double],
    precondition: List[AssessmentMethod]
)
