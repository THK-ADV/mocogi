package parsing.types

import models.core.PO
import java.util.UUID

case class ParsedPrerequisites(
    recommended: Option[ParsedPrerequisiteEntry],
    required: Option[ParsedPrerequisiteEntry]
)

case class ParsedPrerequisiteEntry(
    text: String,
    modules: List[UUID],
    studyPrograms: List[PO]
)
