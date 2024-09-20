package controllers

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import _root_.webhook.GitMergeEventHandler
import _root_.webhook.GitPushEventHandler
import controllers.GitWebhookController.GitlabTokenHeader
import git._
import play.api.libs.json._
import play.api.mvc._

object GitWebhookController {
  val GitlabTokenHeader = "X-Gitlab-Token"
}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    gitConfig: GitConfig,
    gitPushEventHandler: GitPushEventHandler,
    gitMergeEventHandler: GitMergeEventHandler,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def onPushEvent() =
    isAuthenticated(
      Action(parse.json) { implicit r =>
        gitPushEventHandler.handle(r.body)
        NoContent
      }
    )

  def onMergeEvent() =
    isAuthenticated(
      Action(parse.json) { implicit r =>
        gitMergeEventHandler.handle(r.body)
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
