package parsing.types

import basedata.PO

case class ParsedPrerequisites(
    recommended: Option[ParsedPrerequisiteEntry],
    required: Option[ParsedPrerequisiteEntry]
)

case class ParsedPrerequisiteEntry(
    text: String,
    modules: List[String],
    studyPrograms: List[PO]
)
