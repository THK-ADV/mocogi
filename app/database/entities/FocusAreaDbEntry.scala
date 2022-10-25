package database.entities

import basedata.AbbrevLabelDescLike

case class FocusAreaDbEntry(
    abbrev: String,
    program: String,
    deLabel: String,
    enLabel: String,
    deDesc: String,
    enDesc: String
) extends AbbrevLabelDescLike
