package service.mail

import java.util.UUID

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import cats.data.NonEmptyList
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.Props
import play.api.libs.mailer.Email
import play.api.libs.mailer.MailerClient
import play.api.Logging
import service.mail.MailerService.SendMail

class MailerService(private val actor: ActorRef):
  def sendMail(subject: String, bodyText: String, receiver: NonEmptyList[String], cc: List[String] = Nil): Unit =
    actor ! SendMail(subject, bodyText, receiver, cc)

object MailerService {
  private case class SendMail(subject: String, bodyText: String, receiver: NonEmptyList[String], cc: List[String])
  private case class Go(id: UUID, mail: Email, retryCounter: Int)
  private case class Finish(id: UUID)

  def apply(
      system: ActorSystem,
      mailerClient: MailerClient,
      sender: String,
      retries: Int,
      ctx: ExecutionContext
  ): MailerService =
    new MailerService(system.actorOf(Props(new Impl(mailerClient, sender, retries, ctx))))

  private final class Impl(mailerClient: MailerClient, sender: String, retries: Int, implicit val ctx: ExecutionContext)
      extends Actor,
        Logging {
    override def receive = {
      case SendMail(subject, bodyText, receiver, cc) =>
        val id = UUID.randomUUID()
        val mail = Email(
          from = sender,
          to = receiver.toList,
          subject = subject,
          bodyText = Some(bodyText),
          cc = cc
        )
        self ! Go(id, mail, retryCounter = retries)
      case Go(id, mail, retryCounter) if retryCounter > 0 =>
        logger.info(s"[$id] trying to send mail...")
        try {
          mailerClient.send(mail)
          logger.info(s"[$id] mail successfully send")
          self ! Finish(id)
        } catch
          case e: Exception =>
            logger.info(s"[$id] failed to send mail", e)
            scheduleRetry(id, mail, retryCounter)
      case Go(id, _, retryCounter) if retryCounter == 0 =>
        logger.info(s"[$id] no more retries left")
      case Finish(id) =>
        logger.info(s"[$id] finished")
    }

    private def scheduleRetry(id: UUID, mail: Email, retryCounter: Int) = {
      val delay = Math.pow(2, retries - retryCounter + 1).minutes
      logger.info(s"[$id] retrying in $delay")
      context.system.scheduler.scheduleOnce(delay, self, Go(id, mail, retryCounter - 1))
    }
  }
}
