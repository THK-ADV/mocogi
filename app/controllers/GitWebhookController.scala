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
import logging.AppEventLogger
import logging.CorrelationId
import logging.LogEvent
import logging.LogResult
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

  private def dispatchedEventName = "git.webhook.dispatched"

  def onPushMain() =
    isAuthenticated(parse.json) { r =>
      mainPushHandler ! HandleEvent(r.body, r.correlationId)
      AppEventLogger.info(
        logger,
        LogEvent(
          event = dispatchedEventName,
          result = LogResult.Succeeded,
          correlationId = r.correlationId,
          details = Map("webhookType" -> "push_main")
        )
      )
      Future.successful(NoContent)
    }

  def onPushPreview() =
    isAuthenticated(parse.json) { r =>
      previewPushHandler ! HandleEvent(r.body, r.correlationId)
      AppEventLogger.info(
        logger,
        LogEvent(
          event = dispatchedEventName,
          result = LogResult.Succeeded,
          correlationId = r.correlationId,
          details = Map("webhookType" -> "push_preview")
        )
      )
      Future.successful(NoContent)
    }

  def onMerge() =
    isAuthenticated(parse.json) { r =>
      mergeHandler ! HandleEvent(r.body, r.correlationId)
      AppEventLogger.info(
        logger,
        LogEvent(
          event = dispatchedEventName,
          result = LogResult.Succeeded,
          correlationId = r.correlationId,
          details = Map("webhookType" -> "merge")
        )
      )
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
      val correlationId = CorrelationId.random()
      AppEventLogger.info(
        logger,
        LogEvent(
          event = "git.webhook.received",
          result = LogResult.Started,
          correlationId = correlationId
        )
      )
      parseGitToken(r) match {
        case Success(t) =>
          if token == t then action(GitWebhookController.CorrelatedRequest(correlationId, r))
          else {
            AppEventLogger.warn(
              logger,
              LogEvent(
                event = "git.webhook.auth_failed",
                result = LogResult.Failed,
                correlationId = correlationId,
                errorCode = Some("invalid_webhook_token")
              )
            )
            Future.successful(Unauthorized(Json.toJson(new Exception(s"invalid $GitlabTokenHeader"))))
          }
        case Failure(e) =>
          AppEventLogger.warn(
            logger,
            LogEvent(
              event = "git.webhook.auth_failed",
              result = LogResult.Failed,
              correlationId = correlationId,
              errorCode = Some("missing_or_invalid_webhook_header")
            )
          )
          Future.successful(BadRequest(Json.toJson(e)))
      }
    }
  }
}
