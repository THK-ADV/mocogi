package providers

import javax.inject.Inject
import javax.inject.Provider

import ops.ConfigurationOps.Ops
import org.apache.pekko.actor.ActorSystem
import play.api.libs.mailer.MailerClient
import play.api.Configuration
import service.mail.MailerService

final class MailerServiceProvider @Inject() (
    system: ActorSystem,
    mailerClient: MailerClient,
    configuration: Configuration
) extends Provider[MailerService]:
  override def get() = MailerService(system, mailerClient, configuration.nonEmptyString("mail.sender"))
