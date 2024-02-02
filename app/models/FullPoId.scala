package models

case class FullPoId(id: String) extends AnyVal {
  override def toString = id
}

object FullPoId {
  implicit def ord: Ordering[FullPoId] = Ordering.by(_.id)
}
