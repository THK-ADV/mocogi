package parsing.types

import models.core.FocusAreaID
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleECTS(
    value: Double,
    contributionsToFocusAreas: List[ModuleECTSFocusAreaContribution]
)

object ModuleECTS {
  implicit def writes: Writes[ModuleECTS] = Json.writes
}

@Deprecated(forRemoval = true)
case class ModuleECTSFocusAreaContribution(
    focusArea: FocusAreaID,
    ectsValue: Double,
    deDesc: String,
    enDesc: String
)

@Deprecated(forRemoval = true)
object ModuleECTSFocusAreaContribution {
  implicit def writes: Writes[ModuleECTSFocusAreaContribution] = Json.writes
}
