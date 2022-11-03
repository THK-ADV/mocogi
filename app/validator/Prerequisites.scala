package validator

import basedata.PO

case class Prerequisites(
    recommended: Option[PrerequisiteEntry],
    required: Option[PrerequisiteEntry]
)

case class PrerequisiteEntry(
    text: String,
    modules: List[Module],
    pos: List[PO]
)
