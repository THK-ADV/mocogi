package providers

import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

import scala.concurrent.ExecutionContext

import ops.ConfigurationOps.Ops
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.ActorSystem
import play.api.Configuration
import service.notification.ReviewNotificationScheduler

class ReviewNotificationSchedulerProvider @Inject() (
    system: ActorSystem,
    @Named("ReviewNotificationActor") actor: ActorRef,
    configuration: Configuration,
    ctx: ExecutionContext
) extends Provider[ReviewNotificationScheduler] {

  override def get(): ReviewNotificationScheduler =
    new ReviewNotificationScheduler(system, actor, dayOfWeek, fireTime, ctx)

  private def dayOfWeek: DayOfWeek =
    DayOfWeek.of(configuration.int("reviewNotification.day"))

  private def fireTime: LocalTime =
    LocalTime.parse(configuration.nonEmptyString("reviewNotification.time"), DateTimeFormatter.ISO_LOCAL_TIME)
}
