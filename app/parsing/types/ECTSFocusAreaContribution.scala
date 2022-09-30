package parsing.types

import basedata.FocusArea

case class ECTSFocusAreaContribution(
    focusArea: FocusArea,
    ectsValue: Double,
    description: String
)
