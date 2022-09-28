package parsing.types

case class ECTS(
    value: Double,
    contributionsToFocusAreas: List[ECTSFocusAreaContribution]
)
