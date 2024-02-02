package models

case class MergeRequestId(value: Int) extends AnyVal {
  override def toString = value.toString
}
