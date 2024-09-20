package parsing.types

import java.util.UUID

import models.core.PO

case class ParsedPrerequisites(
    recommended: Option[ParsedPrerequisiteEntry],
    required: Option[ParsedPrerequisiteEntry]
)

case class ParsedPrerequisiteEntry(
    text: String,
    modules: List[UUID],
    studyPrograms: List[PO]
)
