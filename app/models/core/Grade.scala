package models.core

case class Grade(
    abbrev: String,
    deLabel: String,
    deDesc: String,
    enLabel: String,
    enDesc: String
) extends AbbrevLabelDescLike

object Grade {
  implicit def ord: Ordering[Grade] = Ordering.by[Grade, String](_.abbrev)
}
