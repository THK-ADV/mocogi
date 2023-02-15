package parsing.types

import models.core.FocusAreaPreview

case class ECTS(
    value: Double,
    contributionsToFocusAreas: List[ECTSFocusAreaContribution]
)

case class ECTSFocusAreaContribution(
    focusArea: FocusAreaPreview,
    ectsValue: Double,
    description: String
)
