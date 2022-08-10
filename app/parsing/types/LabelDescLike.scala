package parsing.types

trait LabelDescLike {
  def abbrev: String
  def deLabel: String
  def deDesc: String
  def enLabel: String
  def enDesc: String
}
