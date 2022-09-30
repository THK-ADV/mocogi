package basedata

case class Competence(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends LabelDescLike
