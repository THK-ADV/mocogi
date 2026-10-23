package service.mail

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class MailConfig(sender: String, maxAttempts: Int = 5)

object MailConfig {
  def load(configuration: Configuration): MailConfig = MailConfig(configuration.nonEmptyString("mail.sender"))
}
