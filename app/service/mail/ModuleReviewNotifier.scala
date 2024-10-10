package service.mail

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import auth.CampusId
import cats.data.NonEmptyList
import database.repo.ModuleReviewRepository
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.Props
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging

final class ModuleReviewNotifier @Inject() (
    system: ActorSystem,
    protected val mailerService: MailerService,
    protected val repo: ModuleReviewRepository,
    protected val messages: MessagesApi,
    protected val dayOfWeek: DayOfWeek,
    fireTime: LocalTime,
    url: String,
)(using ExecutionContext)
    extends Logging:

  private case object NotifyAllPendingReviews

  private val interval = 24.hours
  private val delay    = delayUntil(fireTime)
  private val actor    = system.actorOf(Props(new Impl))

  system.scheduler.scheduleAtFixedRate(
    initialDelay = delay,
    interval = interval,
    receiver = actor,
    message = NotifyAllPendingReviews
  )

  private def delayUntil(fireTime: LocalTime): FiniteDuration = {
    val now         = LocalTime.now
    val diff        = fireTime.toSecondOfDay - now.toSecondOfDay
    val delay: Long = if (diff < 0) interval.toSeconds + diff else diff
    delay.seconds
  }

  protected def buildUrl(module: UUID, review: UUID): String =
    url.replace("$moduleid", module.toString).replace("$approvalid", review.toString)

  private final class Impl extends Actor:

    given Lang(Locale.GERMANY)

    override def receive = {
      case NotifyAllPendingReviews =>
        val currentDayOfWeek = LocalDateTime.now().getDayOfWeek
        if currentDayOfWeek == dayOfWeek then {
          repo.getAllPending().onComplete {
            case Success(xs) =>
              logger.info("Sending emails to all recipients of pending reviews...")
              sendMail(xs)
            case Failure(e) =>
              logger.error("Failed to retrieve all pending review from db", e)
          }
        } else {
          logger.info(s"Wrong day of week: $currentDayOfWeek")
        }
    }

    private def sendMail(reviews: Seq[ModuleReviewRepository.PendingModuleReview])(using Lang): Unit = {
      reviews
        .filter(_.director.id == "ado") // TODO debug
        .groupBy(_.director)
        .foreach {
          case (dir, xs) if dir.campusId.nonEmpty && xs.nonEmpty =>
            val receiver = CampusId(dir.campusId.get).toMailAddress
            val subject  = messages("module_review_notification_subject")
            val body     = StringBuilder()
            body.append(messages("module_review_notification_opening"))
            body.append('\n')
            xs.groupBy(_.module).foreach {
              case (module, xs) =>
                val entry  = xs.head
                val author = s"${entry.moduleAuthor.firstname} ${entry.moduleAuthor.lastname}"
                val roles  = xs.map(_.reviewRole.id.toUpperCase).toSet.mkString(" & ")
                val url    = buildUrl(module.id, entry.reviewId)
                body.append("\n- ")
                body.append(
                  messages(
                    "module_review_notification_body",
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
            body.append(messages("module_review_notification_closing"))
            mailerService.sendMail(subject, body.toString(), NonEmptyList.one(receiver))
          case (dir, _) =>
            logger.info(s"no mail address found for user ${dir.id}")
        }
    }
