package service.notification

import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.NonEmptyList
import database.repo.ModuleDraftRepository
import database.repo.ModuleReviewRepository
import database.repo.ModuleUpdatePermissionRepository
import logging.infoC
import logging.warnC
import logging.CorrelationId
import models.ModuleReview
import org.apache.pekko.actor.ActorRef
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.Logging
import service.mail.MailActor.SendMail
import settings.AppSettings

@Singleton
final class ReviewRejectionNotifier @Inject() (
    moduleReviewRepository: ModuleReviewRepository,
    moduleDraftRepository: ModuleDraftRepository,
    moduleUpdatePermissionRepository: ModuleUpdatePermissionRepository,
    messages: MessagesApi,
    @Named("MailActor") mailActor: ActorRef,
    appSettings: AppSettings,
    implicit val ctx: ExecutionContext
) extends Logging {
  given Lang(Locale.GERMANY)

  def notifyIfSingleRejection(module: UUID)(using CorrelationId): Future[Unit] =
    for
      reviews <- moduleReviewRepository.getAtomicByModule(module)
      rejected = reviews.filter(_.status.isRejected)
      _ <- if rejected.size == 1 then sendMail(module, rejected.head) else Future.unit
    yield ()

  private def sendMail(module: UUID, rejected: ModuleReview.Atomic)(using CorrelationId): Future[Unit] =
    for
      moduleTitle <- moduleDraftRepository.getModuleTitle(module)
      users       <- moduleUpdatePermissionRepository.allPeopleWithPermissionForModule(module)
    yield {
      logger.infoC(s"module review rejected module=$module")
      val body = StringBuilder()
      body.append(
        messages(
          "module_review.rejection.notification.opening",
          moduleTitle,
          rejected.respondedBy.fold("???")(_.fullName),
          appSettings.mail.editUrl.replace("$moduleid", module.toString)
        )
      )
      rejected.comment.foreach { comment =>
        body.append("\n\n")
        val quoted = s"\n${comment.trim}".replaceAll("\n", "\n>")
        body.append(messages("module_review.rejection.notification.reason", quoted))
      }
      body.append("\n\n")
      body.append(messages("module_review.rejection.notification.closing"))

      val to = users.collect { case (person, perm) if perm.isInherited && person.hasEmail => person.email.get }
      val cc = users.collect { case (person, perm) if perm.isGranted && person.hasEmail => person.email.get }

      NonEmptyList.fromList(to.toList) match
        case Some(to) =>
          mailActor ! SendMail(
            messages("module_review.rejection.notification.subject", moduleTitle),
            body.toString(),
            to,
            cc.toList
          )
        case None =>
          logger.warnC(
            s"module review rejection notification skipped module=$module reason=missing_inherited_permissions_recipient"
          )
    }
}
