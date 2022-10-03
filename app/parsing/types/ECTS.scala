package parsing.types

import basedata.FocusArea

case class ECTS(
    value: Double,
    contributionsToFocusAreas: List[ECTSFocusAreaContribution]
)

case class ECTSFocusAreaContribution(
    focusArea: FocusArea,
    ectsValue: Double,
    description: String
)
