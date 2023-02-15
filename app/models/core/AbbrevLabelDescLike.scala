package models.core

trait AbbrevLabelDescLike extends AbbrevLabelLike {
  def abbrev: String
  def deLabel: String
  def deDesc: String
  def enLabel: String
  def enDesc: String
}
