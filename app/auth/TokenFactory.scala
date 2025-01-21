package auth

trait TokenFactory[T] {
  def create(
      attributes: Map[String, AnyRef],
      mail: Option[String],
      roles: Set[String]
  ): Either[String, T]
}
