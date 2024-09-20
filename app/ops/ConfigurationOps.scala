package ops

import play.api.Configuration

object ConfigurationOps {
  final implicit class Ops(private val self: Configuration) extends AnyVal {
    def list(key: String): Seq[String] =
      if (self.has(key)) self.get[Seq[String]](key)
      else throw new Throwable(s"key $key must be set in application.conf")

    def nonEmptyString(key: String): String =
      self.getOptional[String](key) match {
        case Some(value) if value.nonEmpty => value
        case other =>
          throw new Throwable(
            s"expected a non empty string for key $key, but found $other"
          )
      }

    def int(key: String): Int =
      self.getOptional[Int](key) match {
        case Some(value) => value
        case _           => throw new Throwable(s"$key must be set")
      }
  }
}
