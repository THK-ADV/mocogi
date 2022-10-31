package basedata

case class FocusArea(
    abbrev: String,
    program: String,
    deLabel: String,
    enLabel: String,
    deDesc: String,
    enDesc: String
) extends AbbrevLabelDescLike
