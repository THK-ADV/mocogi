package parsing.types

import basedata.FocusAreaPreview

case class ECTS(
    value: Double,
    contributionsToFocusAreas: List[ECTSFocusAreaContribution]
)

case class ECTSFocusAreaContribution(
    focusArea: FocusAreaPreview,
    ectsValue: Double,
    description: String
)
