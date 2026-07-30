package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import _root_.webhook.HandleEvent
import controllers.GitWebhookController.GitlabTokenHeader
import logging.infoC
import logging.warnC
import logging.CorrelationId
import org.apache.pekko.actor.ActorRef
import play.api.libs.json.*
import play.api.mvc.*
import play.api.Logging
import settings.AppSettings

object GitWebhookController {
  val GitlabTokenHeader = "X-Gitlab-Token"

  final case class CorrelatedRequest[A](correlationId: CorrelationId, request: Request[A])
      extends WrappedRequest[A](request)
}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    appSettings: AppSettings,
    @Named("MergeEventHandler") mergeHandler: ActorRef,
    @Named("PreviewPushEventHandler") previewPushHandler: ActorRef,
    @Named("MainPushEventHandler") mainPushHandler: ActorRef,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with Logging {
  private def token: UUID = appSettings.git.webhookToken

  def onPushMain() =
    isAuthenticated(parse.json) { r =>
      given CorrelationId = r.correlationId
      mainPushHandler ! HandleEvent(r.body, r.correlationId)
      logger.infoC("webhook dispatched type=push_main")
      Future.successful(NoContent)
    }

  def onPushPreview() =
    isAuthenticated(parse.json) { r =>
      given CorrelationId = r.correlationId
      previewPushHandler ! HandleEvent(r.body, r.correlationId)
      logger.infoC("webhook dispatched type=push_preview")
      Future.successful(NoContent)
    }

  def onMerge() =
    isAuthenticated(parse.json) { r =>
      given CorrelationId = r.correlationId
      mergeHandler ! HandleEvent(r.body, r.correlationId)
      logger.infoC("webhook dispatched type=merge")
      Future.successful(NoContent)
    }

  private def isAuthenticated[A](parser: BodyParser[A])(
      action: GitWebhookController.CorrelatedRequest[A] => Future[Result]
  ): Action[A] = {
    def parseGitToken(implicit r: Request[?]): Try[UUID] =
      r.headers.get(GitlabTokenHeader) match {
        case Some(s) => Try(UUID.fromString(s))
        case None    => Failure(new Exception(s"expected $GitlabTokenHeader header"))
      }

    Action.async(parser) { r =>
      given CorrelationId = CorrelationId.random()
      parseGitToken(r) match {
        case Success(t) =>
          if token == t then action(GitWebhookController.CorrelatedRequest(summon[CorrelationId], r))
          else {
            logger.warnC("webhook auth failed reason=invalid_token")
            Future.successful(Unauthorized(Json.toJson(new Exception(s"invalid $GitlabTokenHeader"))))
          }
        case Failure(e) =>
          logger.warnC("webhook auth failed reason=missing_or_invalid_header")
          Future.successful(BadRequest(Json.toJson(e)))
      }
    }
  }
}
