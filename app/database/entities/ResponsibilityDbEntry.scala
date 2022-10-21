package database.entities

import database.table.ResponsibilityType

import java.util.UUID

case class ResponsibilityDbEntry(
    metadata: UUID,
    person: String,
    kind: ResponsibilityType
)
