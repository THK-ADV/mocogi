package parsing.types

import models.core.FocusAreaID
import play.api.libs.json.{Json, Writes}

case class ECTS(
    value: Double,
    contributionsToFocusAreas: List[ECTSFocusAreaContribution]
)

object ECTS {
  implicit def writes: Writes[ECTS] = Json.writes
}

case class ECTSFocusAreaContribution(
    focusArea: FocusAreaID,
    ectsValue: Double,
    deDesc: String,
    enDesc: String
)

object ECTSFocusAreaContribution {
  implicit def writes: Writes[ECTSFocusAreaContribution] = Json.writes
}
