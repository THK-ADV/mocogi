package parsing.types

import basedata.AssessmentMethod

case class AssessmentMethods(
    mandatory: List[AssessmentMethodEntry],
    optional: List[AssessmentMethodEntry]
)

case class AssessmentMethodEntry(
    method: AssessmentMethod,
    percentage: Option[Double],
    precondition: List[AssessmentMethod]
)
