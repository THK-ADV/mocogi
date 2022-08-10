package parsing.metadata.file

import parsing.types.LabelDescLike

case class LabelDescImpl(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends LabelDescLike
