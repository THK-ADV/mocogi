package models

import java.util.UUID

case class CreatedModule(
    module: UUID,
    moduleTitle: String,
    moduleAbbrev: String,
    moduleManagement: List[String],
    moduleECTS: Double
)
