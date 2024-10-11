package service.mail

import cats.data.NonEmptyList
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.Props
import play.api.libs.mailer.Email
import play.api.libs.mailer.MailerClient
import play.api.Logging
import MailerService.SendMail

class MailerService(private val actor: ActorRef):
  def sendMail(subject: String, bodyText: String, receiver: NonEmptyList[String]): Unit =
    actor ! SendMail(subject, bodyText, receiver)

object MailerService:
  private case class SendMail(subject: String, bodyText: String, receiver: NonEmptyList[String])

  def apply(system: ActorSystem, mailerClient: MailerClient, sender: String): MailerService =
    new MailerService(system.actorOf(Props(new Impl(mailerClient, sender))))

  private final class Impl(mailerClient: MailerClient, sender: String) extends Actor, Logging:
    override def receive = {
      case SendMail(subject, bodyText, receiver) =>
        val email = Email(
          from = sender,
          to = receiver.toList,
          subject = subject,
          bodyText = Some(bodyText)
        )
        try {
          mailerClient.send(email)
          logger.info(s"Successfully sent email to ${email.to.mkString(", ")}")
        } catch
          case e: Exception =>
            logger.error(s"Failed to sent email to ${email.to.mkString(", ")}", e)
    }
