package parsing.types

case class GlobalCriteria(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends LabelDescLike
