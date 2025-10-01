package service.notification

import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import auth.CampusId
import cats.data.NonEmptyList
import database.repo.ModuleReviewRepository
import org.apache.pekko.actor.Actor
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import service.mail.MailerService
import service.notification.ReviewNotificationActor.DryRun
import service.notification.ReviewNotificationActor.NotifyAllPendingReviews

object ReviewNotificationActor {
  case object NotifyAllPendingReviews
  case object DryRun
}

final class ReviewNotificationActor @Inject() (
    private val repo: ModuleReviewRepository,
    private val messages: MessagesApi,
    @Named("reviewNotificationUrl") private val reviewNotificationUrl: String,
    private val mailerService: MailerService,
    private implicit val ctx: ExecutionContext
) extends Actor
    with Logging {
  given Lang(Locale.GERMANY)

  override def receive = {
    case NotifyAllPendingReviews =>
      repo.getAllPending().onComplete {
        case Success(xs) =>
          logger.info("Sending emails to all recipients of pending reviews...")
        // sendMail(xs)
        case Failure(e) =>
          logger.error("Failed to retrieve all pending review from db", e)
      }
    case DryRun =>
      repo.getAllPending().onComplete {
        case Success(xs) =>
          logger.info("Sending emails to all recipients of pending reviews...")
          sendMail(xs, dryRun = true)
        case Failure(e) =>
          logger.error("Failed to retrieve all pending review from db", e)
      }
  }

  private def buildUrl(module: UUID): String =
    reviewNotificationUrl.replace("$moduleid", module.toString)

  private def sendMail(reviews: Seq[ModuleReviewRepository.PendingModuleReview], dryRun: Boolean = false): Unit = {
    reviews
      .groupBy(_.director)
      .foreach {
        case (dir, xs) if dir.campusId.nonEmpty && xs.nonEmpty =>
          val receiver = CampusId(dir.campusId.get).toMailAddress
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
              val roles = xs.map(_.reviewRole.id.toUpperCase).toSet.mkString(" & ")
              val url   = buildUrl(module.id)
              body.append("\n- ")
              body.append(
                messages(
                  "module_review.notification.body",
                  s"${module.title} (${module.abbrev})",
                  author,
                  s"${entry.reviewStudyProgramLabel} (${entry.reviewDegreeLabel})",
                  roles,
                  url
                )
              )
              body.append('\n')
          }
          body.append("\n")
          body.append(messages("module_review.notification.closing"))

          if dryRun then {
            logger.info(s"An: $receiver")
            logger.info(body.toString())
          } else mailerService.sendMail(subject, body.toString(), NonEmptyList.one(receiver))
        case (dir, _) =>
          logger.info(s"no mail address found for user ${dir.id}")
      }
  }
}
