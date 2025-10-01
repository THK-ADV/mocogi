package service.notification

import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

import scala.concurrent.ExecutionContext

import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import play.api.Logging

final class ReviewNotificationScheduler @Inject() (
    private val system: ActorSystem,
    private val actor: ActorRef,
    private val dayOfWeek: DayOfWeek,
    private val fireTime: LocalTime,
    private implicit val ctx: ExecutionContext
) extends Logging {

//  private val interval = 24.hours
//  private val delay    = delayUntil(fireTime)
//
//  system.scheduler.scheduleAtFixedRate(
//    initialDelay = delay,
//    interval = interval,
//    receiver = actor,
//    message = NotifyAllPendingReviews
//  )
//
//  val currentDayOfWeek = LocalDateTime.now().getDayOfWeek
//  if currentDayOfWeek == dayOfWeek then {
//    repo.getAllPending().onComplete {
//      case Success(xs) =>
//        logger.info("Sending emails to all recipients of pending reviews...")
//        sendMail(xs)
//      case Failure(e) =>
//        logger.error("Failed to retrieve all pending review from db", e)
//    }
//  } else {
//    logger.info(s"Wrong day of week: $currentDayOfWeek")
//  }
//
//  private def delayUntil(fireTime: LocalTime): FiniteDuration = {
//    val now         = LocalTime.now
//    val diff        = fireTime.toSecondOfDay - now.toSecondOfDay
//    val delay: Long = if (diff < 0) interval.toSeconds + diff else diff
//    delay.seconds
//  }
}
