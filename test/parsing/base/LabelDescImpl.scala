package parsing.base

import basedata.LabelDescLike

case class LabelDescImpl(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends LabelDescLike
