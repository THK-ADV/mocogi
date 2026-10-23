package service.mail

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import cats.data.NonEmptyList
import org.apache.pekko.actor.Actor
import play.api.libs.mailer.Email
import play.api.libs.mailer.MailerClient
import play.api.Logging
import service.mail.MailActor.SendMail

object MailActor {
  case class SendMail(subject: String, bodyText: String, receiver: NonEmptyList[String], cc: List[String])
}

final class MailActor @Inject() (
    mailerClient: MailerClient,
    config: MailConfig,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  private case class Go(id: UUID, mail: Email, remainingAttempts: Int)
  private case class Finish(id: UUID)

  override def receive = {
    case SendMail(subject, bodyText, receiver, cc) =>
      val id   = UUID.randomUUID()
      val mail = Email(
        from = config.sender,
        to = receiver.toList,
        subject = subject,
        bodyText = Some(bodyText),
        cc = cc
      )
      self ! Go(id, mail, remainingAttempts = config.maxAttempts)
    case Go(id, mail, remainingAttempts) if remainingAttempts > 0 =>
      logger.info(s"[$id] trying to send mail...")
      try {
        mailerClient.send(mail)
        logger.info(s"[$id] mail successfully send")
        self ! Finish(id)
      } catch
        case e: Exception =>
          logger.info(s"[$id] failed to send mail", e)
          scheduleRetry(id, mail, remainingAttempts)
    case Go(id, _, remainingAttempts) if remainingAttempts == 0 =>
      logger.info(s"[$id] no attempts left")
    case Finish(id) =>
      logger.info(s"[$id] finished")
  }

  private def scheduleRetry(id: UUID, mail: Email, remainingAttempts: Int) = {
    val delay = Math.pow(2, config.maxAttempts - remainingAttempts + 1).minutes
    logger.info(s"[$id] retrying in $delay")
    context.system.scheduler.scheduleOnce(delay, self, Go(id, mail, remainingAttempts - 1))
  }
}
