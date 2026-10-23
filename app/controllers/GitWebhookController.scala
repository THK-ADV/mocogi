package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import webhook.HandleEvent
import webhook.WebhookAuthenticator
import webhook.WebhookConfig
import webhook.WebhookEndpoint
import webhook.WebhookEvent
import auth.AuthorizationAction
import com.fasterxml.jackson.core.JsonProcessingException
import controllers.actions.UserResolveAction
import database.repo.PermissionRepository
import database.repo.WebhookInboxRepository
import logging.infoC
import logging.CorrelationId
import org.apache.pekko.actor.ActorRef
import permission.AdminCheck
import permission.Role
import permission.ServiceAccountCheck
import play.api.libs.json.*
import play.api.mvc.*
import play.api.Logging
import security.ClientErrorResponse

@Singleton
final class GitWebhookController @Inject() (
    cc: ControllerComponents,
    config: WebhookConfig,
    inbox: WebhookInboxRepository,
    auth: AuthorizationAction,
    @Named("MergeEventHandler") mergeHandler: ActorRef,
    @Named("PreviewPushEventHandler") previewPushHandler: ActorRef,
    @Named("MainPushEventHandler") mainPushHandler: ActorRef,
    val permissionRepository: PermissionRepository,
    val clientErrors: ClientErrorResponse,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with AdminCheck
    with ServiceAccountCheck
    with UserResolveAction
    with Logging {
  private val authenticator = WebhookAuthenticator(config)

  def onPushMain() = receive(WebhookEndpoint.PushMain)

  def onPushPreview() = receive(WebhookEndpoint.PushPreview)

  def onMerge() = receive(WebhookEndpoint.Merge)

  private def receive(endpoint: WebhookEndpoint) =
    Action.async(parse.byteString(config.maxBodyBytes)) { r =>
      given CorrelationId = CorrelationId.random()
      authenticator.verify(endpoint, r.headers, r.body) match {
        case Left(rejection) =>
          Future.successful(
            clientErrors.clientStatusError(r, UNAUTHORIZED, s"webhook rejected type=${endpoint.id} reason=$rejection")
          )
        case Right(eventId) =>
          def reject(reason: String) =
            Future.successful(
              clientErrors.badRequest(r, s"webhook rejected ${fields(endpoint, eventId)} reason=$reason")
            )
          if (!r.contentType.contains("application/json")) reject("unsupported_content_type")
          else
            try {
              val event = WebhookEvent(eventId, endpoint, Json.parse(r.body.toArray))
              if (matchesEndpoint(event)) accept(event) else reject("unexpected_project_or_event_type")
            } catch { case _: JsonProcessingException => reject("invalid_json") }
      }
    }

  private def accept(event: WebhookEvent)(using CorrelationId) =
    inbox.accept(event).map { accepted =>
      if (accepted) dispatch(event)
      else logger.infoC(s"webhook duplicate skipped ${fields(event.endpoint, event.id)}")
      NoContent
    }

  /**
   * Dispatches the stored event of `eventId` again.
   *
   * GitLab's "Resend Request" keeps the `webhook-id` and is therefore skipped as duplicate.
   */
  def replay(eventId: UUID) =
    auth.andThen(resolveUser).andThen(isAdmin).async(parse.empty) { r =>
      given CorrelationId = CorrelationId.random()
      inbox.find(eventId).map {
        case Some(event) =>
          logger.infoC(
            s"webhook replay requested ${fields(event.endpoint, eventId)} username=${r.request.token.username}"
          )
          dispatch(event)
          Accepted(Json.obj("correlationId" -> summon[CorrelationId].value.toString))
        case None =>
          clientErrors.clientStatusError(
            r,
            NOT_FOUND,
            s"webhook event_id=$eventId was never accepted, use GitLab's Resend Request instead"
          )
      }
    }

  def deleteExpired() =
    auth.andThen(hasRole(Role.DeleteExpiredWebhooks)).async { _ =>
      given CorrelationId = CorrelationId.random()
      inbox.deleteExpired(config.inboxRetention).map { deleted =>
        logger.infoC(s"expired webhooks deleted count=$deleted")
        NoContent
      }
    }

  /**
   * Checks the project and event type. Branches are left to the handlers, since they skip
   * irrelevant branches themselves and the merge handler also type-checks MRs into other branches.
   */
  private def matchesEndpoint(event: WebhookEvent): Boolean =
    (event.payload \ "object_kind").asOpt[String].contains(event.endpoint.objectKind) &&
      (event.payload \ "project" \ "id").asOpt[Int].contains(config.projectId)

  private def dispatch(event: WebhookEvent)(using CorrelationId): Unit = {
    val handler = event.endpoint match {
      case WebhookEndpoint.Merge       => mergeHandler
      case WebhookEndpoint.PushMain    => mainPushHandler
      case WebhookEndpoint.PushPreview => previewPushHandler
    }
    handler ! HandleEvent(event.payload, summon[CorrelationId])
    logger.infoC(s"webhook dispatched ${fields(event.endpoint, event.id)}")
  }

  private def fields(endpoint: WebhookEndpoint, eventId: UUID) = s"type=${endpoint.id} event_id=$eventId"
}
