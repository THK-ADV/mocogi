package service

case class Print(value: String) extends AnyVal {
  override def toString = value
}
