package ops

import play.api.Configuration

object ConfigurationOps {
  implicit final class Ops(private val self: Configuration) extends AnyVal {
    def list(key: String): Seq[String] =
      if (self.has(key)) self.get[Seq[String]](key)
      else throw new Throwable(s"key $key must be set in application.conf")

    def nonEmptyString(key: String): String =
      self.getOptional[String](key) match {
        case Some(value) if value.nonEmpty => value
        case _ => throw new Throwable(s"$key must be set")
      }

    def emptyString(key: String): Option[String] =
      self.getOptional[String](key) match {
        case Some(value) if value.nonEmpty => Some(value)
        case _                             => None
      }

    def int(key: String): Int =
      self.getOptional[Int](key) match {
        case Some(value) => value
        case _           => throw new Throwable(s"$key must be set")
      }
  }
}
