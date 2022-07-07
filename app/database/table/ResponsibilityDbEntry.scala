package database.table

import java.util.UUID

case class ResponsibilityDbEntry(
    metadata: UUID,
    person: String,
    kind: ResponsibilityType
)
