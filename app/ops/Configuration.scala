package ops

import play.api.Configuration

extension (self: Configuration) {
  def list(key: String): Seq[String] =
    if self.has(key) then self.get[Seq[String]](key)
    else throw new Exception(s"key $key must be set in application.conf")

  def nonEmptyString(key: String): String =
    self.getOptional[String](key) match {
      case Some(value) if value.nonEmpty => value
      case other                         => throw new Exception(s"expected a non empty string for key $key, but found $other")
    }

  def int(key: String): Int =
    self.getOptional[Int](key) match {
      case Some(value) => value
      case _           => throw new Exception(s"$key must be set")
    }
}
