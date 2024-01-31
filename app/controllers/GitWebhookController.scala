package controllers

import controllers.GitWebhookController.GitlabTokenHeader
import git._
import play.api.libs.json._
import play.api.mvc._
import _root_.webhook.GitPushEventHandler

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object GitWebhookController {
  val GitlabTokenHeader = "X-Gitlab-Token"
}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    gitConfig: GitConfig,
    gitMergeEventHandlingActor: GitPushEventHandler,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def onPushEvent() =
    isAuthenticated(
      Action(parse.json) { implicit r =>
        gitMergeEventHandlingActor.handle(r.body)
        NoContent
      }
    )

  def onMergeEvent() =
    isAuthenticated(
      Action(parse.json) { implicit r =>
        gitMergeEventHandlingActor.handle(r.body)
        NoContent
      }
    )

  private def isAuthenticated[A](action: Action[A]) = {
    def parseGitToken(implicit r: Request[_]): Try[UUID] =
      r.headers.get(GitlabTokenHeader) match {
        case Some(s) => Try(UUID.fromString(s))
        case None =>
          Failure(new Throwable(s"expected $GitlabTokenHeader header"))
      }

    Action.async(action.parser) { r =>
      parseGitToken(r) match {
        case Success(t) =>
          if (gitConfig.gitToken.fold(true)(_ == t)) action(r)
          else
            Future.successful(
              Unauthorized(
                Json.toJson(new Throwable(s"invalid $GitlabTokenHeader"))
              )
            )
        case Failure(e) =>
          Future.successful(BadRequest(Json.toJson(e)))
      }
    }
  }
}
