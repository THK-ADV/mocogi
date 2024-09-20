package models.core

trait IDLabelDesc extends IDLabel {
  def deDesc: String
  def enDesc: String
}

object IDLabelDesc {
  def apply(
      _id: String,
      _deLabel: String,
      _enLabel: String,
      _deDesc: String,
      _enDesc: String
  ): IDLabelDesc =
    new IDLabelDesc {
      override def id: String      = _id
      override def deLabel: String = _deLabel
      override def enLabel: String = _enLabel
      override def deDesc: String  = _deDesc
      override def enDesc: String  = _enDesc
    }
}
