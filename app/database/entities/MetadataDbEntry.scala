package database.entities

import java.util.UUID

case class MetadataDbEntry(
    id: UUID,
    gitPath: String,
    title: String,
    abbrev: String,
    moduleType: String,
    language: String
)
