package auth

trait TokenFactory[UserToken] {
  def create(
      attributes: Map[String, AnyRef],
      mail: String,
      roles: Set[String]
  ): Either[String, UserToken]
}
