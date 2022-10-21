package parsing.base

import basedata.AbbrevLabelDescLike

case class AbbrevLabelDescImpl(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends AbbrevLabelDescLike
