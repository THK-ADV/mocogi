package ops

import play.api.Configuration

object ConfigurationOps {
  extension (configuration: Configuration) {
    def nonEmptyString(key: String): String = {
      val value = configuration.get[String](key)
      require(value.nonEmpty, s"$key must not be empty")
      value
    }
  }
}
