package service.notification

import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import auth.CampusId
import cats.data.NonEmptyList
import database.repo.ModuleReviewRepository
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import service.mail.MailActor
import service.mail.MailActor.SendMail
import service.notification.ReviewNotificationActor.DryRun
import service.notification.ReviewNotificationActor.NotifyAllPendingReviews

object ReviewNotificationActor {
  case object NotifyAllPendingReviews
  case object DryRun
}

final class ReviewNotificationActor @Inject() (
    repo: ModuleReviewRepository,
    messages: MessagesApi,
    @Named("MailActor") mailActor: ActorRef,
    @Named("reviewNotificationUrl") moduleReviewUrl: String,
    private implicit val ctx: ExecutionContext
) extends Actor
    with Logging {
  given Lang(Locale.GERMANY)

  override def receive = {
    case NotifyAllPendingReviews => go(dryRun = false)
    case DryRun                  => go(dryRun = true)
  }

  private def go(dryRun: Boolean): Unit = {
    repo.getAllPending().onComplete {
      case Success(xs) =>
        val prefix = if dryRun then "[DRY] " else ""
        logger.info(s"${prefix}Sending emails to all recipients of pending reviews...")
        sendMail(xs, dryRun)
      case Failure(e) =>
        logger.error("Failed to retrieve all pending review from db", e)
    }
  }

  private def sendMail(reviews: Seq[ModuleReviewRepository.PendingModuleReview], dryRun: Boolean = false): Unit = {
    reviews
      .groupBy(_.director)
      .foreach {
        case (dir, xs) if dir.campusId.nonEmpty && xs.nonEmpty =>
          val subject  = messages("module_review.notification.subject")
          val body     = StringBuilder()
          body.append(messages("module_review.notification.opening"))
          body.append('\n')

          xs.groupBy(_.module).foreach {
            case (module, xs) =>
              val entry = xs.head
              val author = (entry.moduleAuthor.firstname, entry.moduleAuthor.lastname) match {
                case (Some(firstname), Some(lastname)) => s"$firstname $lastname"
                case _                                 => entry.moduleAuthor.id
              }
              body.append("\n- ")
              body.append(
                messages(
                  "module_review.notification.body",
                  s"${module.title} (${module.abbrev})",
                  author,
                  s"${entry.reviewStudyProgramLabel} (${entry.reviewDegreeLabel})",
                )
              )
          }
          body.append("\n\n")
          body.append(messages("module_review.notification.action", moduleReviewUrl))
          body.append("\n\n")
          body.append(messages("module_review.notification.closing"))

          if dryRun then {
            val receiver = "alexander.dobrynin@th-koeln.de"
            mailActor ! SendMail(subject, body.toString(), NonEmptyList.one(receiver), Nil)
          } else {
            val receiver = CampusId(dir.campusId.get).toMailAddress
            mailActor ! SendMail(subject, body.toString(), NonEmptyList.one(receiver), Nil)
          }
        case (dir, _) =>
          logger.info(s"no mail address found for user ${dir.id}")
      }
  }
}
