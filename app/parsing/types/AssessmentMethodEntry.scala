package parsing.types

import basedata.AssessmentMethod

case class AssessmentMethodEntry(
    method: AssessmentMethod,
    percentage: Option[Double],
    precondition: List[AssessmentMethod]
)
