package models.core

case class GlobalCriteria(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends AbbrevLabelDescLike
