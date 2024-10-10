package providers

import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Provider

import scala.concurrent.ExecutionContext

import database.repo.ModuleReviewRepository
import ops.ConfigurationOps.Ops
import org.apache.pekko.actor.ActorSystem
import play.api.i18n.MessagesApi
import play.api.Configuration
import service.mail.MailerService
import service.mail.ModuleReviewNotifier

class ModuleReviewNotifierProvider @Inject() (
    system: ActorSystem,
    mailerService: MailerService,
    repo: ModuleReviewRepository,
    messagesApi: MessagesApi,
    configuration: Configuration,
    ctx: ExecutionContext
) extends Provider[ModuleReviewNotifier] {
  override def get(): ModuleReviewNotifier =
    new ModuleReviewNotifier(system, mailerService, repo, messagesApi, dayOfWeek, fireTime, url)(using ctx)

  private def dayOfWeek: DayOfWeek =
    DayOfWeek.of(configuration.int("reviewNotification.day"))

  private def fireTime: LocalTime =
    LocalTime.parse(configuration.nonEmptyString("reviewNotification.time"), DateTimeFormatter.ISO_LOCAL_TIME)

  private def url: String =
    configuration.nonEmptyString("reviewNotification.url")
}
