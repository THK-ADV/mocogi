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
import org.apache.pekko.actor.ActorRef
import play.api.libs.json.*
import play.api.mvc.*

object GitWebhookController {
  val GitlabTokenHeader = "X-Gitlab-Token"
}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    @Named("webhookToken") token: UUID,
    @Named("MergeEventHandler") mergeHandler: ActorRef,
    @Named("PreviewPushEventHandler") previewPushHandler: ActorRef,
    @Named("MainPushEventHandler") mainPushHandler: ActorRef,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def onPushMain() =
    isAuthenticated(
      Action(parse.json) { implicit r =>
        mainPushHandler ! HandleEvent(r.body)
        NoContent
      }
    )

  def onPushPreview() =
    isAuthenticated(
      Action(parse.json) { implicit r =>
        previewPushHandler ! HandleEvent(r.body)
        NoContent
      }
    )

  def onMerge() =
    isAuthenticated(
      Action(parse.json) { implicit r =>
        mergeHandler ! HandleEvent(r.body)
        NoContent
      }
    )

  private def isAuthenticated[A](action: Action[A]) = {
    def parseGitToken(implicit r: Request[?]): Try[UUID] =
      r.headers.get(GitlabTokenHeader) match {
        case Some(s) => Try(UUID.fromString(s))
        case None    => Failure(new Exception(s"expected $GitlabTokenHeader header"))
      }

    Action.async(action.parser) { r =>
      parseGitToken(r) match {
        case Success(t) =>
          if token == t then action(r)
          else Future.successful(Unauthorized(Json.toJson(new Exception(s"invalid $GitlabTokenHeader"))))
        case Failure(e) => Future.successful(BadRequest(Json.toJson(e)))
      }
    }
  }
}
